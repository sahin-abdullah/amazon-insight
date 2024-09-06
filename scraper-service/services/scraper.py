import asyncio
import logging
import re
from pytz import timezone
import random
from datetime import datetime
from typing import Optional, List, Tuple
from flask import current_app
from playwright.async_api import async_playwright, Page, ElementHandle, Browser, BrowserContext
from playwright_stealth import stealth_async
from amazoncaptcha import AmazonCaptcha
from fake_useragent import UserAgent

from models import AmazonItem, Review, UserTask
from utils import db
from utils import task_statuses

from transformers import AutoTokenizer, AutoModel

# Set up logging
logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
)

ua = UserAgent()


class AmazonScraperService:

    def __init__(self):
        self.ASIN_PATTERN = re.compile(r"/(dp|product)/([A-Z0-9]{10})")
        self.DATE_PATTERN = re.compile(r"[A-Z][a-z]*(\. | )[0-9]{1,2}, [0-9]{4}")
        self.HELPFUL_PATTERN = re.compile(r"^(?:(\d+) people|One person)\s+found this helpful")
        self.RATING_TEXT_PATTERN = re.compile(r"(?<!\d)(\d{1,3}(?:,\d{3})*|\d+)(?!\d)")
        self.RATING_PATTERN = re.compile(r"^(\d+(\.\d)?)")
        self.FILTER_BY_STAR = ["one_star", "two_star", "three_star", "four_star", "five_star"]
        self.BASE_URL = "https://www.amazon.com/product-reviews/{}/ref=cm_cr_dp_show_all_btm?ie=UTF8&reviewerType=all_reviews&filterByStar={}&sortBy=recent&pageNumber={}"
        self.MAX_PAGE = 3
        self.MAX_RETRIES = 3
        self.RETRY_DELAY_MS = 5000
        self.progress = 0.0

    async def get_item_reviews(self, asin: str, task_id: str):
        with current_app.app_context():
            review_list = []

            # Randomize User-Agent and other headers
            user_agent = ua.random
            custom_headers = {
                "Referer": "https://www.amazon.com/",
                "Accept-Language": "en-US,en;q=0.9",
                "Accept-Encoding": "gzip, deflate, br",
                "Upgrade-Insecure-Requests": "1",
                "Sec-Fetch-Dest": "document",
                "Sec-Fetch-Mode": "navigate",
                "Sec-Fetch-Site": "same-origin",
                "Sec-Fetch-User": "?1",
                "Connection": "keep-alive"
            }

            browser: Optional[Browser] = None
            context: Optional[BrowserContext] = None

            try:
                await asyncio.sleep(0)
                async with async_playwright() as p:
                    browser = await p.chromium.launch(headless=True)
                    context = await browser.new_context(
                        user_agent=user_agent,
                        extra_http_headers=custom_headers
                    )

                    # Navigate to the first page and handle CAPTCHA
                    page = await self.navigate_and_handle_captcha(context, self.BASE_URL.format(asin, "", 1))
                    if not page:
                        logging.error("Failed to navigate to the initial page after CAPTCHA.")
                        return

                    # Apply stealth techniques
                    await stealth_async(page)

                    # Extract item properties
                    title = await self.extract_title(page)
                    average_rating = await self.extract_overall_rating(page)
                    total_review_count = await self.extract_total_review_count(page)
                    total_rating_count = await self.extract_total_rating_count(page)
                    image_url = await self.capture_image_source(asin)
                    item_features = await self.capture_product_features(asin)

                    # Create or update the item in the database
                    item = self.create_or_update_item(asin, title, average_rating,
                                                      total_review_count,
                                                      total_rating_count, image_url,
                                                      item_features)

                    self.progress += 0.1
                    task_statuses[task_id] = self.progress

                    # Fetch and process reviews for each star rating
                    for ind, star_filter in enumerate(self.FILTER_BY_STAR):
                        await self.process_reviews_for_star_rating(context, item, star_filter,
                                                                   task_id, ind)

            except Exception as e:
                logging.error("Error in get_item_reviews", exc_info=e)
                raise

            finally:
                logging.info("Entering finally block")
                self.progress = 1
                task_statuses[task_id] = self.progress

                # Update the end_date for the UserTask record
                self.update_user_task_end_date(task_id)

                if browser:
                    try:
                        await browser.close()
                        logging.info("Browser closed successfully")
                    except Exception as e:
                        logging.error(f"Error during browser closure: {e}")

                if browser:
                    try:
                        await browser.close()
                        logging.info("Browser closed successfully")
                    except Exception as e:
                        logging.error(f"Error during browser closure: {e}")

    def create_or_update_item(self, asin, title, average_rating, total_review_count,
                              total_rating_count, image_url, item_features):
        """Creates a new item or updates an existing one in the database."""
        item = AmazonItem.query.filter_by(asin=asin).first()
        if not item:
            item = AmazonItem(
                asin=asin,
                title=title,
                average_rating=average_rating,
                total_review_count=total_review_count,
                total_rating_count=total_rating_count,
                image_url=image_url,
                item_features=item_features,
                reviews=[]
            )
            db.session.add(item)
        else:
            # Update existing item properties if needed
            item.title = title
            item.average_rating = average_rating
            item.total_review_count = total_review_count
            item.total_rating_count = total_rating_count
            item.image_url = image_url
            item.item_features = item_features
        db.session.commit()
        return item

    async def process_reviews_for_star_rating(self, context, item, star_filter,
                                              task_id, ind):
        """Processes reviews for a specific star rating."""
        page_count = 1
        total_star_ratings = len(self.FILTER_BY_STAR)
        progress_per_star_rating = 0.9 / total_star_ratings
        while page_count <= self.MAX_PAGE:
            page_url = self.BASE_URL.format(item.asin, star_filter, page_count)
            page = await self.navigate_and_handle_captcha(context, page_url)
            if not page:
                logging.error(
                    f"Failed to navigate to page: {page_count} with filter: {star_filter} after CAPTCHA."
                )
                break

            await stealth_async(page)

            review_count = await self.extract_total_review_count(page)
            if review_count == 0:
                break

            max_page = min(self.MAX_PAGE, (review_count + 9) // 10)
            logging.info(f"Max Page for {star_filter}: {max_page} and {review_count} Reviews")

            review_elements = await self.fetch_reviews(page)
            logging.info(f"Visiting page {page_count} for {star_filter}")

            for element in review_elements:
                await self.process_review(element, item)

            # Commit changes after processing all reviews on the current page
            db.session.commit()

            is_last_page = await page.locator(
                "#cm_cr-pagination_bar > ul > li.a-disabled.a-last").count()
            if is_last_page or page_count >= max_page:
                break

            page_count += 1
            # Update progress based on processed pages within the star rating
            self.progress += progress_per_star_rating / max_page
            task_statuses[task_id] = self.progress

    async def process_review(self, element, item):
        """Processes a single review element, checks for duplicates,
        and saves the processed review to the database.
        """
        review = await self.extract_review(element)
        if review:
            existing_review = Review.query.filter_by(
                amazon_review_id=review.amazon_review_id
            ).first()
            if existing_review:
                logging.info(
                    f"Review with ID {review.amazon_review_id} already exists. Skipping."
                )
                return

            review_processor = ReviewProcessor()
            processed_reviews = await review_processor.process_and_save_reviews(
                [
                    (
                        review.review_title,
                        review.review_text,
                        review.helpful_count,
                        review.amazon_review_id,
                        review.rating,
                        review.review_date,
                    )
                ]
            )

            for processed_review in processed_reviews:
                processed_review.product = item
                db.session.add(processed_review)

    def update_user_task_end_date(self, task_id):
        """Updates the end date of the user task."""
        with current_app.app_context():
            user_task = UserTask.query.filter_by(task_id=task_id).first()
            if user_task:
                user_task.end_date = datetime.now(timezone('US/Eastern'))
                try:
                    db.session.commit()
                    logging.info(f"Updated end_date for task_id {task_id}")
                except Exception as e:
                    logging.error("Error during end_date update commit", exc_info=e)
                    db.session.rollback()

    async def fetch_reviews(self, page: Page) -> list[ElementHandle]:
        selectors = [
            "div[data-hook='review']",
            # "div[data-hook='mobley-review-content']",
            "div[data-cel-widget^='customer_review']"
        ]

        review_elements = []

        try:
            await asyncio.sleep(0)
            for selector in selectors:
                review_elements = await page.query_selector_all(selector)
                if review_elements:
                    break
        except Exception as e:
            logging.error("Error in fetch_reviews", exc_info=e)

        return review_elements

    async def navigate_and_handle_captcha(self, context: BrowserContext, url: str, page: Optional[Page] = None) -> \
            Optional[Page]:
        if page is None:
            page = await context.new_page()

        # Randomize viewport size
        await page.set_viewport_size({
            "width": random.randint(1200, 1400),
            "height": random.randint(800, 1200)
        })

        # Add a slight delay before navigating (to simulate human behavior)
        await asyncio.sleep(random.uniform(2, 5))

        for retry in range(self.MAX_RETRIES):
            try:
                await asyncio.sleep(0)
                await page.goto(url)  # Set an explicit timeout

                if await self.is_captcha_page(page):
                    if await self.handle_captcha(page):
                        return page  # CAPTCHA handled successfully, returning page
                    else:
                        logging.error(f"Failed to handle CAPTCHA after {retry + 1} retries.")
                        return None  # Indicate navigation failed

                if await self.is_amazon_error_page(page):
                    logging.warning(f"Encountered Amazon error page (attempt {retry + 1}). Retrying...")
                else:
                    return page  # Navigation successful, returning page

            except Exception as e:
                if retry < self.MAX_RETRIES - 1:
                    logging.warning(f"Error during navigation (attempt {retry + 1}): {e}. Retrying after delay...")
                    await asyncio.sleep(self.RETRY_DELAY_MS / 1000)
                else:
                    logging.error(f"Failed to navigate to page: {url} after {self.MAX_RETRIES} retries. Error: {e}")
                    return None  # Indicate navigation failed

        return None  # Navigation failed after all retries

    async def is_amazon_error_page(self, page: Page) -> bool:
        return "Page Not Found" in await page.title()

    async def is_captcha_page(self, page: Page) -> bool:
        return await page.locator("div.a-box.a-alert.a-alert-info h4").count() > 0

    async def handle_captcha(self, page: Page) -> bool:
        captcha_image_url = await self.get_captcha_image_url(page)
        if not captcha_image_url:
            logging.warning("CAPTCHA image URL not found")
            return False

        logging.info(f"CAPTCHA image URL: {captcha_image_url}")
        # await page.screenshot(path="./screenshot.png")

        try:
            captcha = AmazonCaptcha.fromlink(captcha_image_url)
            solution = captcha.solve()
            logging.info(f"Solution of Captcha Image: {solution}")

            captcha_input = page.locator("#captchacharacters")
            await captcha_input.fill(solution)
            logging.info("CAPTCHA filled successfully.")

            submit_button = page.locator("button[type='submit']")
            await submit_button.click()

            return not await self.is_captcha_page(page)
        except Exception as e:
            logging.error(f"Error solving CAPTCHA: {e}")
            return False

    async def get_captcha_image_url(self, page: Page) -> Optional[str]:
        captcha_image = await page.locator(
            "img[src^='https://images-na.ssl-images-amazon.com/captcha/']").get_attribute("src")
        return captcha_image

    async def extract_review(self, element: ElementHandle) -> Optional[Review]:
        try:
            review_id = await self.extract_review_id(element)

            # Select the correct span for the review title
            review_title_locator = await element.query_selector("a[data-hook='review-title'] span:nth-of-type(2)")
            if not review_title_locator or not await review_title_locator.is_visible():
                review_title_locator = await element.query_selector("span[data-hook='review-title'] span")
                if not review_title_locator or not await review_title_locator.is_visible():
                    logging.info("Skipping review because review title is not found.")
                    return None

            title = await review_title_locator.inner_text()
            text = await (await element.query_selector("span[data-hook='review-body']")).inner_text()
            rating = await self.extract_review_rating(element)
            helpful_count = await self.extract_helpful_count(element)
            review_date = await self.extract_review_date(element)

            return Review(
                amazon_review_id=review_id,
                review_title=title.strip(),
                review_text=text.strip(),
                rating=rating,
                helpful_count=helpful_count,
                review_date=review_date
            )
        except Exception as e:
            logging.error("Error while extracting review details.", e)
            return None

    async def extract_review_id(self, review_element: ElementHandle) -> Optional[str]:
        review_id = await review_element.get_attribute("id")
        if review_id is not None:
            # Remove the prefix if it exists
            review_id = review_id.replace("customer_review-", "").replace("customer_review_foreign", "")
        return review_id

    async def extract_review_rating(self, element: ElementHandle) -> float:
        rating_text = await (
            await element.query_selector("i[data-hook='review-star-rating'] span.a-icon-alt")).inner_text()
        match = self.RATING_PATTERN.match(rating_text)
        return float(match.group(1)) if match else 0.0

    async def extract_review_date(self, element: ElementHandle) -> Optional[datetime.date]:
        date_text = await (await element.query_selector("span[data-hook='review-date']")).inner_text()
        match = self.DATE_PATTERN.search(date_text)
        return datetime.strptime(match.group(), "%B %d, %Y").date() if match else None

    async def extract_helpful_count(self, element: ElementHandle) -> Optional[int]:
        """Extracts helpful count from an ElementHandle."""
        helpful_element = await element.query_selector("span[data-hook='helpful-vote-statement']")

        if helpful_element is None:
            # If the element is not found, return 0 (assuming no helpful votes)
            return 0

        helpful_text = await helpful_element.inner_text()
        match = self.HELPFUL_PATTERN.search(helpful_text)

        if match:
            # Return the number of people who found the review helpful, or 1 if only one person did
            return int(match.group(1)) if match.group(1) else 1 if "One person" in match.group(0) else None

        return 0  # Default to 0 if no match

    async def capture_image_source(self, asin: str) -> Optional[str]:
        amazon_url = f"https://www.amazon.com/dp/{asin}"
        try:
            async with async_playwright() as p:
                browser = await p.chromium.launch(headless=True)
                context = await browser.new_context()
                page = await context.new_page()

                # Navigate to the product page
                await page.goto(amazon_url)

                # Apply stealth techniques
                await stealth_async(page)

                # Capture the image source
                image_element = await page.query_selector("#landingImage")
                if image_element:
                    image_url = await image_element.get_attribute("src")
                    logging.info(f"Captured Image URL: {image_url}")
                    return image_url
                else:
                    logging.error("Image element not found.")
                    return None
        except Exception as e:
            logging.error(f"Error capturing image source: {e}")
            return None

    async def capture_product_features(self, asin: str) -> Optional[str]:
        amazon_url = f"https://www.amazon.com/dp/{asin}"
        try:
            async with async_playwright() as p:
                browser = await p.chromium.launch(headless=True)
                context = await browser.new_context()
                page = await context.new_page()

                # Navigate to the product page
                await page.goto(amazon_url)

                # Apply stealth techniques
                await stealth_async(page)

                # Capture the product features
                feature_element = await page.query_selector("#feature-bullets")
                if feature_element:
                    features = await feature_element.inner_text()
                    logging.info(f"Captured Product Features: {features}")
                    return features
                else:
                    logging.error("Feature element not found.")
                    return None
        except Exception as e:
            logging.error(f"Error capturing product features: {e}")
            return None

    async def extract_title(self, page: Page) -> str:
        title = ""

        # First attempt: Extract from the original location
        try:
            # await page.locator("a[data-hook='product-link']").wait_for(state="visible", timeout=3000)
            title = await page.locator("a[data-hook='product-link']").inner_text(timeout=3000)
        except Exception as e:
            logging.warning(f"Title not found using product link: {e}")

        # Second attempt: Extract from the breadcrumb area
        if not title:  # Only attempt if the first one fails
            try:
                # await page.locator("div[data-hook='breadcrumb'] h4").wait_for(state="visible", timeout=3000)
                title = await page.locator("div[data-hook='breadcrumb'] h4").inner_text(timeout=3000)
            except Exception as e:
                logging.warning(f"Title not found using breadcrumb: {e}")

        # Third attempt: Extract from the page <title> tag
        if not title:  # Only attempt if both above fail
            try:
                title = await page.title()
            except Exception as e:
                logging.warning(f"Title not found in the <title> tag: {e}")

        # Clean up the title by removing unwanted parts
        if title:
            title = title.replace("Amazon.com: Customer reviews:", "").strip()

            # Further cleaning can be done using regular expressions if needed
            # Example: Remove "Customer reviews: " prefix using regex
            title = re.sub(r"^Customer reviews:\s*", "", title)

        # If all attempts fail, return an empty string or a default value
        if not title:
            logging.error("Failed to extract the product title from all known locations.")
            return ""

        return title

    async def extract_overall_rating(self, page: Page) -> float:
        rating_text = ""

        # First attempt: Extract rating from the original location
        try:
            rating_text = await page.locator("span[data-hook='rating-out-of-text']").inner_text(timeout=3000)
        except Exception as e:
            logging.warning(f"Rating not found using 'rating-out-of-text': {e}")

        # Second attempt: Extract rating from the alternative location
        if not rating_text:  # Only attempt if the first one fails
            try:
                rating_text = await page.locator("span[data-hook='average-stars-rating-text']").inner_text(timeout=3000)
            except Exception as e:
                logging.warning(f"Rating not found using 'average-stars-rating-text': {e}")

        # If rating text is still empty, return 0.0
        if not rating_text:
            logging.error("Failed to extract the rating from all known locations.")
            return 0.0

        # Extract the numeric rating using a regular expression
        match = re.search(r"(\d+(\.\d)?)", rating_text)
        return float(match.group(1)) if match else 0.0

    async def extract_total_review_count(self, page: Page) -> int:
        text = ""

        # First attempt: Extract from the original location
        try:
            text = await page.locator("div[data-hook='cr-filter-info-review-rating-count']").inner_text(timeout=3000)
        except Exception as e:
            logging.warning(f"Total review count not found using 'cr-filter-info-review-rating-count': {e}")

        # Second attempt: Extract from an alternative location in the filter info section
        if not text:
            try:
                text = await page.locator("div#reviews-filter-info div[data-hook='cr-filter-info-review-rating-count']").inner_text()
            except Exception as e:
                logging.warning(f"Total review count not found using 'reviews-filter-info': {e}")

        # If text is still empty, return 0
        if not text:
            logging.error("Failed to extract the total review count from all known locations.")
            return 0

        # Extract the numeric value using a regular expression
        matches = [match.group(1) for match in self.RATING_TEXT_PATTERN.finditer(text)]
        return int(matches[1].replace(",", "")) if len(matches) > 1 else 0

    async def extract_total_rating_count(self, page: Page) -> int:
        text = ""

        # First attempt: Extract from the original location
        try:
            text = (await page.locator("div[data-hook='cr-filter-info-review-rating-count']").inner_text(timeout=3000)).strip()
        except Exception as e:
            logging.warning(f"Total rating count not found using 'cr-filter-info-review-rating-count': {e}")

        # Second attempt: Extract from an alternative location in the filter info section
        if not text:
            try:
                text = (await page.locator("div#reviews-filter-info div[data-hook='cr-filter-info-review-rating-count']").inner_text(timeout=3000)).strip()
            except Exception as e:
                logging.warning(f"Total rating count not found using 'reviews-filter-info': {e}")

        # If text is still empty, return 0
        if not text:
            logging.error("Failed to extract the total rating count from all known locations.")
            return 0

        # Extract the numeric value using a regular expression
        matches = [match.group(1) for match in self.RATING_TEXT_PATTERN.finditer(text)]
        return int(matches[0].replace(",", "")) if matches else 0


class ReviewProcessor:

    def __init__(self, embedding_model_name="sentence-transformers/all-mpnet-base-v1"):
        self.tokenizer = AutoTokenizer.from_pretrained(embedding_model_name)
        self.embedding_model = AutoModel.from_pretrained(embedding_model_name)

    def clean_text(self, text: str) -> str:
        """Cleans up review text."""
        text = text.lower()
        text = re.sub(r'http[s]?://\S+', '', text)
        text = re.sub(r'<.*?>', '', text)
        text = re.sub(r'\s+', ' ', text).strip()
        return text

    def generate_embedding_for_review(self, title: str, review_text: str, helpful_count: int):
        """Generates a single embedding vector for a review."""

        cleaned_title = self.clean_text(title)
        cleaned_review_text = self.clean_text(review_text)

        text_to_embed = (
            f"[Title] {cleaned_title} "
            f"[Review] {cleaned_review_text} "
            f"[Helpfulness] This review was found helpful by {helpful_count} people."
        )

        inputs = self.tokenizer(text_to_embed, return_tensors="pt", truncation=True, padding=True, max_length=512)
        embeddings = self.embedding_model(**inputs).last_hidden_state.mean(dim=1)

        return embeddings[0].cpu().detach().numpy()

    async def process_and_save_reviews(self, reviews: List[Tuple]):
        """Processes reviews, generates embeddings, and returns them."""

        processed_reviews = []
        for review_data in reviews:
            review_title, review_text, helpful_count, amazon_review_id, rating, review_date = review_data
            embedding = self.generate_embedding_for_review(review_title, review_text, helpful_count)

            review = Review(
                review_title=review_title,
                review_text=review_text,
                helpful_count=helpful_count,
                amazon_review_id=amazon_review_id,
                rating=rating,
                review_date=review_date,
                embedding=embedding
            )
            processed_reviews.append(review)

        return processed_reviews
