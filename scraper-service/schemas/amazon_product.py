from marshmallow import Schema, fields


class ReviewSchema(Schema):
    amazonReviewID = fields.Str(required=True)
    reviewTitle = fields.Str()
    reviewText = fields.Str()
    rating = fields.Float()
    helpfulCount = fields.Int()
    reviewDate = fields.Date()


class AmazonItemSchema(Schema):
    asin = fields.Str(required=True)
    title = fields.Str()
    averageRating = fields.Float()
    totalReviewCount = fields.Int()
    totalRatingCount = fields.Int()
    imageUrl = fields.Str()
    itemFeatures = fields.Str()
    reviews = fields.List(fields.Nested(ReviewSchema))
