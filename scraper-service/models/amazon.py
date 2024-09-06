from utils import db
from sqlalchemy.orm import relationship
from pgvector.sqlalchemy import Vector


class AmazonItem(db.Model):
    __tablename__ = 'amazon_item'

    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    asin = db.Column(db.String(10), nullable=False, unique=True)
    title = db.Column(db.String, nullable=True)
    average_rating = db.Column(db.Float, nullable=True)
    total_review_count = db.Column(db.Integer, nullable=True)
    total_rating_count = db.Column(db.Integer, nullable=True)
    image_url = db.Column(db.String, nullable=True)
    item_features = db.Column(db.Text, nullable=True)

    reviews = relationship('Review', back_populates='product', cascade='all, delete-orphan', lazy='dynamic')

    def __repr__(self):
        return f"<AmazonItem(asin='{self.asin}', title='{self.title}')>"


class Review(db.Model):
    __tablename__ = 'reviews'

    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    amazon_review_id = db.Column('amazon_reviewid', db.String(255), unique=True, nullable=False)
    review_title = db.Column('review_title', db.String(255), nullable=True)
    review_text = db.Column('review_text', db.Text, nullable=True)
    rating = db.Column(db.Float, nullable=True)
    helpful_count = db.Column('helpful_count', db.Integer, nullable=True)
    review_date = db.Column('review_date', db.Date, nullable=True)

    # Embedding Column
    embedding = db.Column(Vector(dim=768), nullable=True)  # Adjust dim based on your model

    product_id = db.Column(db.Integer, db.ForeignKey('amazon_item.id'), nullable=False)
    product = relationship('AmazonItem', back_populates='reviews')

    def __repr__(self):
        return f"<Review(amazon_review_id='{self.amazon_review_id}', review_title='{self.review_title}')>"
