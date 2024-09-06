from flask_smorest import Blueprint
from services.relevant_reviews import ReviewSearchService
from schemas import RelevantReviewsRequestSchema, RelevantReviewsResponseSchema

blp = Blueprint('relevant_reviews', __name__)
review_search_service = ReviewSearchService()


@blp.route('/relevant-reviews', methods=['POST'])
@blp.arguments(RelevantReviewsRequestSchema)
@blp.response(200, RelevantReviewsResponseSchema)
def get_relevant_reviews(data):
    question = data['question']
    prod_id = data['id']
    top_n = data.get('top_n', 5)

    # Find the most relevant reviews using the question and asin
    relevant_reviews = review_search_service.find_relevant_reviews(question, prod_id, top_n)

    # Extract only the review IDs
    relevant_reviews_data = [review.id for review in relevant_reviews]

    return {'amazonReviewIDs': relevant_reviews_data}
