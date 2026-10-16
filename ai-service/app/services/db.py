import json
from contextlib import contextmanager
from typing import Any, Dict, List

import pymysql

from app.config import settings


@contextmanager
def mysql_connection():
    conn = pymysql.connect(
        host=settings.mysql_host,
        port=settings.mysql_port,
        user=settings.mysql_user,
        password=settings.mysql_password,
        database=settings.mysql_database,
        charset=settings.mysql_charset,
        cursorclass=pymysql.cursors.DictCursor,
    )
    try:
        yield conn
    finally:
        conn.close()


class ProductRepository:
    def fetch_products(self) -> List[Dict[str, Any]]:
        sql = """
        SELECT
            p.id,
            p.name,
            p.description,
            p.price,
            p.stock,
            p.discount_price AS discountPrice,
            p.sales_count AS salesCount,
            p.place_of_origin AS placeOfOrigin,
            p.status,
            c.name AS category,
            IFNULL(r.reviewCount, 0) AS reviewCount,
            IFNULL(r.avgRating, 0) AS avgRating,
            IFNULL(r.goodReviews, '') AS reviewSummary
        FROM product p
        LEFT JOIN category c ON p.category_id = c.id
        LEFT JOIN (
            SELECT
                product_id,
                COUNT(*) AS reviewCount,
                AVG(rating) AS avgRating,
                GROUP_CONCAT(content ORDER BY created_at DESC SEPARATOR '；') AS goodReviews
            FROM review
            WHERE status = 1
            GROUP BY product_id
        ) r ON r.product_id = p.id
        WHERE p.status = 1
        ORDER BY p.sales_count DESC, p.updated_at DESC
        """
        with mysql_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(sql)
                return list(cursor.fetchall())

    def log_ai_call(self, function_name: str, request_data: Dict[str, Any], response_data: Dict[str, Any], provider: str, success: bool, error_message: str = "") -> None:
        sql = """
        INSERT INTO ai_call_log(function_name, request_text, response_text, provider, success, error_message)
        VALUES(%s, %s, %s, %s, %s, %s)
        """
        try:
            with mysql_connection() as conn:
                with conn.cursor() as cursor:
                    cursor.execute(
                        sql,
                        (
                            function_name,
                            json.dumps(request_data, ensure_ascii=False),
                            json.dumps(response_data, ensure_ascii=False),
                            provider,
                            1 if success else 0,
                            error_message[:1000],
                        ),
                    )
                    conn.commit()
        except Exception:
            # Optional table may not be imported; logging must never block the business flow.
            return
