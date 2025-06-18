from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional
from enum import Enum
import uuid
from datetime import datetime

app = FastAPI()

# Disable CORS. Do not remove this for full-stack development.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allows all origins
    allow_credentials=True,
    allow_methods=["*"],  # Allows all methods
    allow_headers=["*"],  # Allows all headers
)

models_db: List[dict] = []

class BusinessLine(str, Enum):
    RETAIL_BANKING = "Retail Banking"
    WHOLESALE_LENDING = "Wholesale Lending"
    INVESTMENT_BANKING = "Investment Banking"
    RISK_MANAGEMENT = "Risk Management"

class ModelType(str, Enum):
    CREDIT_RISK = "Credit Risk"
    MARKET_RISK = "Market Risk"
    OPERATIONAL_RISK = "Operational Risk"
    AML = "AML"
    CAPITAL_CALCULATION = "Capital Calculation"
    VALUATION = "Valuation"

class RiskRating(str, Enum):
    HIGH = "High"
    MEDIUM = "Medium"
    LOW = "Low"

class Status(str, Enum):
    IN_DEVELOPMENT = "In Development"
    VALIDATED = "Validated"
    PRODUCTION = "Production"
    RETIRED = "Retired"

class ModelCreate(BaseModel):
    model_name: str = Field(..., min_length=1, description="Model name is required")
    model_version: str = Field(..., min_length=1, description="Model version is required")
    model_sponsor: str = Field(..., min_length=1, description="Model sponsor is required")
    business_line: BusinessLine = Field(..., description="Business line is required")
    model_type: ModelType = Field(..., description="Model type is required")
    risk_rating: RiskRating = Field(..., description="Risk rating is required")
    status: Status = Field(..., description="Status is required")

class ModelResponse(BaseModel):
    model_id: str
    model_name: str
    model_version: str
    model_sponsor: str
    business_line: str
    model_type: str
    risk_rating: str
    status: str
    created_at: str

@app.get("/healthz")
async def healthz():
    return {"status": "ok"}

@app.post("/api/models", response_model=ModelResponse)
async def create_model(model: ModelCreate):
    """Create a new model with auto-generated unique ID"""
    try:
        model_id = str(uuid.uuid4())
        
        model_record = {
            "model_id": model_id,
            "model_name": model.model_name,
            "model_version": model.model_version,
            "model_sponsor": model.model_sponsor,
            "business_line": model.business_line.value,
            "model_type": model.model_type.value,
            "risk_rating": model.risk_rating.value,
            "status": model.status.value,
            "created_at": datetime.utcnow().isoformat()
        }
        
        models_db.append(model_record)
        
        return ModelResponse(**model_record)
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error creating model: {str(e)}")

@app.get("/api/models", response_model=List[ModelResponse])
async def get_models():
    """Retrieve all registered models"""
    try:
        return [ModelResponse(**model) for model in models_db]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving models: {str(e)}")

@app.get("/api/models/count")
async def get_models_count():
    """Get the count of registered models"""
    return {"count": len(models_db)}
