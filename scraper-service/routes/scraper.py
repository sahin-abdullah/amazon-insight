import re
import uuid
import asyncio
from flask import Flask
from flask_executor import Executor
from flask.views import MethodView
from flask_smorest import Blueprint, abort

from schemas import UrlSchema
from services import AmazonScraperService
from utils import task_statuses

app = Flask(__name__)

# Add the required configuration
app.config['EXECUTOR_PUSH_APP_CONTEXT'] = True

# Initialize the executor
executor = Executor(app)

blp = Blueprint(
    "amazon scraper api",
    import_name=__name__,
    description="Scraper API for Amazon Insight",
    url_prefix="/scraper",
)


def extract_asin(url):
    match = re.search(r"/(dp|product)/([A-Z0-9]{10})", url)
    if match:
        return match.group(2)
    return None


def run_scraper(asin, task_id):
    amazon_scraper = AmazonScraperService()
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    loop.run_until_complete(amazon_scraper.get_item_reviews(asin, task_id))


@blp.route("/")
class AmazonScraper(MethodView):
    @blp.arguments(UrlSchema)
    @blp.response(200, description="Amazon Insight URL", schema=None)
    def post(self, data):
        url = data['url']

        if not url:
            abort(404, message="URL is required field")

        asin = extract_asin(url)

        if not asin:
            abort(404, message="URL is not valid")

        # Create a unique task ID
        task_id = str(uuid.uuid4())

        # Initialize the status
        task_statuses[task_id] = "Started"

        # Submit the task to the executor
        executor.submit(run_scraper, asin, task_id)

        # Return the task ID to the client
        return {"message": "Scraping started", "task_id": task_id}, 202
