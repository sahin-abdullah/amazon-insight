from transformers import AutoTokenizer, AutoModel
from typing import List
from sqlalchemy import func
from models import Review
from utils import db


class ReviewSearchService:
    def __init__(self, embedding_model_name="sentence-transformers/all-mpnet-base-v1"):
        self.tokenizer = AutoTokenizer.from_pretrained(embedding_model_name)
        self.embedding_model = AutoModel.from_pretrained(embedding_model_name)

    def generate_embedding(self, text: str):
        """Generates an embedding for the given text."""
        inputs = self.tokenizer(text, return_tensors="pt", truncation=True, padding=True, max_length=512)
        embeddings = self.embedding_model(**inputs).last_hidden_state.mean(dim=1)
        return embeddings[0].cpu().detach().numpy()

    def find_relevant_reviews(self, question: str, prod_id: int, top_n: int = 5) -> List[Review]:
        """Finds the most relevant reviews using SQLAlchemy's database functions."""

        question_embedding = self.generate_embedding(question).tolist()

        # Use SQLAlchemy's func.l2_distance for database-level calculation
        closest_reviews = db.session.query(Review).filter(
            Review.product_id == prod_id  # Assuming each review has a 'product_id' attribute
        ).order_by(
            Review.embedding.op('<->')(question_embedding)
        ).limit(top_n).all()

        return closest_reviews
