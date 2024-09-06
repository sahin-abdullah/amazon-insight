from marshmallow import Schema, fields


class UrlSchema(Schema):
    url = fields.Url(required=True, description="URL of the Amazon Products")
    