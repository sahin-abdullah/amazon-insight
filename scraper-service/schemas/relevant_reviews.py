from marshmallow import Schema, fields


class RelevantReviewsRequestSchema(Schema):
    question = fields.Str(required=True)
    id = fields.Integer(required=True)
    top_n = fields.Int(missing=5)


class RelevantReviewsResponseSchema(Schema):
    amazonReviewIDs = fields.List(fields.Integer())
