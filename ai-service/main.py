from fastapi import FastAPI
from pydantic import BaseModel, Field, ConfigDict
from typing import List

app = FastAPI(title="SmartTicket AI Service")


# ---------------- MOVIE DATA MODEL ----------------

class MovieData(BaseModel):
    id: int
    title: str
    description: str = ""
    genre: str = ""
    language: str = ""
    duration: int | None = None
    releaseDate: str = ""
    posterUrl: str = ""


# ---------------- RECOMMENDATION REQUEST ----------------

class RecommendationRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    userId: int = Field(alias="user_id")
    favoriteGenres: List[str] = Field(
        default_factory=list,
        alias="favorite_genres"
    )
    limit: int = 5
    movies: List[MovieData] = Field(default_factory=list)


# ---------------- DEMAND PREDICTION REQUEST ----------------

class DemandPredictionRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    showId: int = Field(alias="show_id")
    currentBookings: int = Field(alias="current_bookings")
    totalSeats: int = Field(alias="total_seats")
    hoursUntilShow: float = Field(alias="hours_until_show")


# ---------------- FRAUD DETECTION REQUEST ----------------

class FraudDetectionRequest(BaseModel):
    bookingId: int
    userId: int
    bookingAmount: float
    bookingFrequency: int
    failedPaymentAttempts: int
    bookingsInLastMinute: int


# ---------------- HEALTH CHECK ----------------

@app.get("/health")
def health():
    return {
        "status": "UP",
        "service": "SmartTicket AI"
    }


# ---------------- AI MOVIE RECOMMENDATIONS ----------------

@app.post("/api/ai/recommendations")
def recommendations(request: RecommendationRequest):

    favorite_genres = {
        genre.strip().lower()
        for genre in request.favoriteGenres
        if genre.strip()
    }

    scored_movies = []

    for movie in request.movies:

        genre = movie.genre.lower()
        description = movie.description.lower()

        score = 0

        for favorite in favorite_genres:

            if favorite in genre:
                score += 10

            if favorite in description:
                score += 3

        if score > 0:

            scored_movies.append({
                "id": movie.id,
                "title": movie.title,
                "description": movie.description,
                "genre": movie.genre,
                "language": movie.language,
                "posterUrl": movie.posterUrl,
                "score": score
            })

    scored_movies.sort(
        key=lambda movie: movie["score"],
        reverse=True
    )

    recommendations_list = [
        {
            key: value
            for key, value in movie.items()
            if key != "score"
        }
        for movie in scored_movies[:max(0, request.limit)]
    ]

    return {
        "userId": request.userId,
        "recommendations": recommendations_list,
        "totalRecommendations": len(recommendations_list),
        "algorithm": "Content-Based Filtering"
    }


# ---------------- AI DEMAND PREDICTION ----------------

@app.post("/api/ai/demand-prediction")
def predict_demand(request: DemandPredictionRequest):

    occupancy = (
        request.currentBookings / request.totalSeats
        if request.totalSeats > 0 else 0
    )

    return {
    "showId": request.showId,
    "totalSeats": request.totalSeats,
    "currentBookings": request.currentBookings,
    "hoursUntilShow": round(request.hoursUntilShow, 1),
    "occupancyRate": round(occupancy * 100, 2),
    "predictedDemand": (
        "HIGH" if occupancy >= 0.75
        else "MEDIUM" if occupancy >= 0.40
        else "LOW"
    )
}


# ---------------- AI FRAUD DETECTION ----------------

@app.post("/api/ai/fraud-detection")
def detect_fraud(request: FraudDetectionRequest):

    suspicious = (
        request.failedPaymentAttempts >= 3
        or request.bookingsInLastMinute >= 5
    )

    return {
        "bookingId": request.bookingId,
        "fraudDetected": suspicious,
        "riskLevel": "HIGH" if suspicious else "LOW"
    }