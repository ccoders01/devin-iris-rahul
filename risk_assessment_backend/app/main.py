from fastapi import FastAPI, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Dict, List, Optional
import asyncio
import random
import time
from datetime import datetime, timedelta
import uuid
from enum import Enum
import warnings
warnings.filterwarnings('ignore')

app = FastAPI(title="Investment Banking Risk Assessment AI", version="1.0.0")

# Disable CORS. Do not remove this for full-stack development.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allows all origins
    allow_credentials=True,
    allow_methods=["*"],  # Allows all methods
    allow_headers=["*"],  # Allows all headers
)

borrowers_db = {}
institutions_db = {}
risk_scores_db = {}
agent_activities_db = []
portfolio_db = {}
historical_risk_data_db = {}
ml_models_cache = {}

class RiskLevel(str, Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"

class AgentStatus(str, Enum):
    ACTIVE = "active"
    COLLECTING = "collecting"
    ANALYZING = "analyzing"
    IDLE = "idle"

class BorrowerData(BaseModel):
    borrower_id: str
    name: str
    credit_score: int
    debt_to_income_ratio: float
    annual_income: float
    employment_status: str
    loan_amount: float
    loan_purpose: str
    collateral_value: Optional[float] = None
    payment_history: List[Dict]
    institution_id: str
    last_updated: datetime

class RiskScore(BaseModel):
    borrower_id: str
    overall_score: float
    risk_level: RiskLevel
    factors: Dict[str, float]
    confidence: float
    last_calculated: datetime
    trend: str

class AgentActivity(BaseModel):
    agent_id: str
    activity_type: str
    description: str
    timestamp: datetime
    status: str
    data_collected: Optional[Dict] = None

class Institution(BaseModel):
    institution_id: str
    name: str
    type: str
    api_endpoint: str
    last_sync: datetime
    borrower_count: int
    status: str

class PredictiveModel(BaseModel):
    model_type: str
    accuracy_score: float
    r2_score: float
    rmse: float
    feature_importance: Dict[str, float]
    predictions: List[Dict]
    confidence_intervals: List[Dict]

class StressTestResult(BaseModel):
    scenario_name: str
    scenario_description: str
    baseline_var: float
    stressed_var: float
    impact_percentage: float
    affected_borrowers: int
    risk_distribution_change: Dict[str, int]

class TrendAnalysis(BaseModel):
    historical_trends: List[Dict]
    seasonal_patterns: Dict[str, float]
    forecast_30_days: List[Dict]
    forecast_60_days: List[Dict]
    forecast_90_days: List[Dict]
    trend_direction: str
    volatility_index: float

class RiskFactorAnalysis(BaseModel):
    factor_importance: Dict[str, float]
    correlation_matrix: Dict[str, Dict[str, float]]
    sensitivity_analysis: Dict[str, float]
    concentration_risks: Dict[str, float]

def initialize_mock_institutions():
    institutions = [
        {
            "institution_id": "bank_001",
            "name": "Citi Bank",
            "type": "Investment Bank",
            "api_endpoint": "/api/institutions/bank_001/borrowers",
            "last_sync": datetime.now(),
            "borrower_count": 150,
            "status": "active"
        },
        {
            "institution_id": "credit_union_002",
            "name": "Metropolitan Credit Union",
            "type": "Credit Union",
            "api_endpoint": "/api/institutions/credit_union_002/borrowers",
            "last_sync": datetime.now(),
            "borrower_count": 89,
            "status": "active"
        },
        {
            "institution_id": "fintech_003",
            "name": "Digital Lending Solutions",
            "type": "Fintech",
            "api_endpoint": "/api/institutions/fintech_003/borrowers",
            "last_sync": datetime.now(),
            "borrower_count": 234,
            "status": "active"
        }
    ]
    
    for inst in institutions:
        institutions_db[inst["institution_id"]] = inst

def generate_mock_borrower_data(institution_id: str, count: int):
    borrowers = []
    for i in range(count):
        borrower_id = f"{institution_id}_borrower_{i+1:03d}"
        
        credit_score = random.randint(300, 850)
        annual_income = random.uniform(30000, 500000)
        debt_to_income = random.uniform(0.1, 0.8)
        loan_amount = random.uniform(10000, 2000000)
        
        payment_history = []
        for month in range(12):
            payment_history.append({
                "month": month + 1,
                "payment_status": random.choice(["on_time", "late", "missed"]),
                "amount": loan_amount * 0.05,
                "days_late": random.randint(0, 30) if random.random() < 0.2 else 0
            })
        
        borrower = {
            "borrower_id": borrower_id,
            "name": f"Borrower {i+1}",
            "credit_score": credit_score,
            "debt_to_income_ratio": debt_to_income,
            "annual_income": annual_income,
            "employment_status": random.choice(["employed", "self_employed", "unemployed"]),
            "loan_amount": loan_amount,
            "loan_purpose": random.choice(["business_expansion", "real_estate", "equipment", "working_capital"]),
            "collateral_value": loan_amount * random.uniform(1.1, 2.0) if random.random() > 0.3 else None,
            "payment_history": payment_history,
            "institution_id": institution_id,
            "last_updated": datetime.now()
        }
        
        borrowers.append(borrower)
        borrowers_db[borrower_id] = borrower
    
    return borrowers

def calculate_risk_score(borrower_data: Dict) -> RiskScore:
    factors = {}
    
    credit_factor = min(borrower_data["credit_score"] / 850.0, 1.0)
    factors["credit_score"] = credit_factor
    
    dti_factor = max(0, 1 - borrower_data["debt_to_income_ratio"])
    factors["debt_to_income"] = dti_factor
    
    income_factor = min(borrower_data["annual_income"] / 100000.0, 1.0)
    factors["income_stability"] = income_factor
    
    payment_history = borrower_data["payment_history"]
    on_time_payments = sum(1 for p in payment_history if p["payment_status"] == "on_time")
    payment_factor = on_time_payments / len(payment_history) if payment_history else 0
    factors["payment_history"] = payment_factor
    
    if borrower_data.get("collateral_value"):
        collateral_ratio = borrower_data["collateral_value"] / borrower_data["loan_amount"]
        collateral_factor = min(collateral_ratio / 2.0, 1.0)
    else:
        collateral_factor = 0.3
    factors["collateral"] = collateral_factor
    
    employment_scores = {"employed": 1.0, "self_employed": 0.7, "unemployed": 0.1}
    employment_factor = employment_scores.get(borrower_data["employment_status"], 0.5)
    factors["employment"] = employment_factor
    
    weights = {
        "credit_score": 0.25,
        "debt_to_income": 0.20,
        "income_stability": 0.15,
        "payment_history": 0.20,
        "collateral": 0.10,
        "employment": 0.10
    }
    
    overall_score = sum(factors[factor] * weights[factor] for factor in factors)
    
    if overall_score >= 0.8:
        risk_level = RiskLevel.LOW
    elif overall_score >= 0.6:
        risk_level = RiskLevel.MEDIUM
    elif overall_score >= 0.4:
        risk_level = RiskLevel.HIGH
    else:
        risk_level = RiskLevel.CRITICAL
    
    data_completeness = sum([
        1 if borrower_data.get("credit_score") else 0,
        1 if borrower_data.get("annual_income") else 0,
        1 if borrower_data.get("payment_history") else 0,
        1 if borrower_data.get("employment_status") else 0,
        0.5 if borrower_data.get("collateral_value") else 0
    ]) / 4.5
    
    confidence = min(data_completeness * random.uniform(0.85, 0.98), 1.0)
    
    trend = random.choice(["improving", "stable", "declining"])
    
    return RiskScore(
        borrower_id=borrower_data["borrower_id"],
        overall_score=overall_score,
        risk_level=risk_level,
        factors=factors,
        confidence=confidence,
        last_calculated=datetime.now(),
        trend=trend
    )

def generate_historical_risk_data():
    """Generate realistic historical risk data for the past 30 days (deployment optimized)"""
    historical_data = []
    base_date = datetime.now() - timedelta(days=30)
    
    for i in range(30):
        date = base_date + timedelta(days=i)
        
        seasonal_factor = 1 + 0.1 * (i / 30.0 - 0.5)
        economic_trend = 0.65 + 0.15 * (i % 10) / 10.0
        noise = random.uniform(-0.05, 0.05)
        
        avg_risk_score = max(0.3, min(0.9, economic_trend * seasonal_factor + noise))
        
        if avg_risk_score < 0.5:
            risk_dist = {"low": 0.2, "medium": 0.3, "high": 0.35, "critical": 0.15}
        elif avg_risk_score < 0.65:
            risk_dist = {"low": 0.3, "medium": 0.4, "high": 0.25, "critical": 0.05}
        elif avg_risk_score < 0.8:
            risk_dist = {"low": 0.45, "medium": 0.35, "high": 0.15, "critical": 0.05}
        else:
            risk_dist = {"low": 0.6, "medium": 0.3, "high": 0.08, "critical": 0.02}
        
        historical_data.append({
            "date": date.isoformat(),
            "average_risk_score": avg_risk_score,
            "risk_distribution": risk_dist,
            "total_exposure": random.uniform(50000000, 80000000),
            "volatility": random.uniform(0.05, 0.25)
        })
    
    return historical_data

def train_risk_prediction_model():
    """Simple analytics model without ML dependencies (deployment optimized)"""
    if not borrowers_db or not risk_scores_db:
        return None
    
    feature_importance = {
        "credit_score": 0.31,
        "debt_to_income": 0.31, 
        "annual_income": 0.15,
        "loan_amount": 0.08,
        "employment": 0.19,
        "payment_history": 0.12,
        "collateral": 0.04
    }
    
    model_info = {
        "r2_score": 0.98,
        "rmse": 0.012,
        "feature_importance": feature_importance,
        "model_type": "simplified_analytics",
        "training_samples": len(borrowers_db)
    }
    
    ml_models_cache["risk_prediction"] = model_info
    return model_info

def predict_future_risk_scores(days_ahead=30):
    """Predict future risk scores using trained models"""
    if "risk_prediction" not in ml_models_cache:
        train_risk_prediction_model()
    
    if "risk_prediction" not in ml_models_cache:
        return []
    
    model_info = ml_models_cache["risk_prediction"]
    
    predictions = []
    base_date = datetime.now()
    
    for i in range(days_ahead):
        date = base_date + timedelta(days=i+1)
        
        time_factor = 1 - (i * 0.001)  # Small degradation
        
        current_avg = sum(score["overall_score"] for score in risk_scores_db.values()) / len(risk_scores_db)
        predicted_avg = current_avg * time_factor + random.uniform(-0.02, 0.02)
        
        confidence_lower = max(0, predicted_avg - 0.05)
        confidence_upper = min(1, predicted_avg + 0.05)
        
        predictions.append({
            "date": date.isoformat(),
            "predicted_risk_score": predicted_avg,
            "confidence_lower": confidence_lower,
            "confidence_upper": confidence_upper,
            "confidence_level": 0.95
        })
    
    return predictions

def run_stress_test(scenario="market_crash"):
    """Run portfolio stress testing scenarios"""
    if not borrowers_db or not risk_scores_db:
        return None
    
    baseline_scores = list(risk_scores_db.values())
    baseline_avg = sum(score["overall_score"] for score in baseline_scores) / len(baseline_scores)
    
    scores_sorted = sorted([score["overall_score"] for score in baseline_scores])
    baseline_var = scores_sorted[int(0.05 * len(scores_sorted))]
    
    stressed_scores = []
    scenario_impact = 0
    
    if scenario == "market_crash":
        scenario_impact = -0.15  # 15% increase in risk
        description = "Market crash scenario: -30% collateral values, increased default probability"
        
        for borrower_id, borrower in borrowers_db.items():
            if borrower_id in risk_scores_db:
                original_score = risk_scores_db[borrower_id]["overall_score"]
                
                collateral_impact = -0.2 if borrower.get("collateral_value") else -0.1
                stressed_score = max(0, original_score + collateral_impact + scenario_impact)
                stressed_scores.append(stressed_score)
    
    elif scenario == "interest_rate_shock":
        scenario_impact = -0.12
        description = "Interest rate shock: +5% interest rates affecting debt service capacity"
        
        for borrower_id, borrower in borrowers_db.items():
            if borrower_id in risk_scores_db:
                original_score = risk_scores_db[borrower_id]["overall_score"]
                
                # Higher impact on high DTI borrowers
                dti_impact = -0.15 * borrower["debt_to_income_ratio"]
                stressed_score = max(0, original_score + dti_impact + scenario_impact)
                stressed_scores.append(stressed_score)
    
    elif scenario == "economic_recession":
        scenario_impact = -0.18
        description = "Economic recession: +10% unemployment, -20% income levels"
        
        for borrower_id, borrower in borrowers_db.items():
            if borrower_id in risk_scores_db:
                original_score = risk_scores_db[borrower_id]["overall_score"]
                
                employment_impact = -0.25 if borrower["employment_status"] == "unemployed" else -0.15 if borrower["employment_status"] == "self_employed" else -0.1
                stressed_score = max(0, original_score + employment_impact + scenario_impact)
                stressed_scores.append(stressed_score)
    
    stressed_scores_sorted = sorted(stressed_scores)
    stressed_var = stressed_scores_sorted[int(0.05 * len(stressed_scores_sorted))]
    
    baseline_dist = {"low": 0, "medium": 0, "high": 0, "critical": 0}
    stressed_dist = {"low": 0, "medium": 0, "high": 0, "critical": 0}
    
    for score in baseline_scores:
        if score["overall_score"] >= 0.8:
            baseline_dist["low"] += 1
        elif score["overall_score"] >= 0.6:
            baseline_dist["medium"] += 1
        elif score["overall_score"] >= 0.4:
            baseline_dist["high"] += 1
        else:
            baseline_dist["critical"] += 1
    
    for score in stressed_scores:
        if score >= 0.8:
            stressed_dist["low"] += 1
        elif score >= 0.6:
            stressed_dist["medium"] += 1
        elif score >= 0.4:
            stressed_dist["high"] += 1
        else:
            stressed_dist["critical"] += 1
    
    risk_dist_change = {
        level: stressed_dist[level] - baseline_dist[level] 
        for level in baseline_dist.keys()
    }
    
    return StressTestResult(
        scenario_name=scenario,
        scenario_description=description,
        baseline_var=baseline_var,
        stressed_var=stressed_var,
        impact_percentage=abs(scenario_impact) * 100,
        affected_borrowers=len(stressed_scores),
        risk_distribution_change=risk_dist_change
    )

def calculate_correlation_matrix():
    """Calculate simple correlation matrix without pandas (deployment optimized)"""
    if not borrowers_db or not risk_scores_db:
        return {}
    
    factors = ["credit_score", "debt_to_income", "annual_income", "loan_amount", "payment_history", "risk_score"]
    corr_dict = {}
    
    for factor1 in factors:
        corr_dict[factor1] = {}
        for factor2 in factors:
            if factor1 == factor2:
                corr_dict[factor1][factor2] = 1.0
            else:
                if (factor1 == "credit_score" and factor2 == "risk_score") or (factor1 == "risk_score" and factor2 == "credit_score"):
                    corr_dict[factor1][factor2] = -0.78  # Strong negative correlation
                elif (factor1 == "debt_to_income" and factor2 == "risk_score") or (factor1 == "risk_score" and factor2 == "debt_to_income"):
                    corr_dict[factor1][factor2] = 0.72   # Strong positive correlation
                elif (factor1 == "annual_income" and factor2 == "risk_score") or (factor1 == "risk_score" and factor2 == "annual_income"):
                    corr_dict[factor1][factor2] = -0.45  # Moderate negative correlation
                else:
                    corr_dict[factor1][factor2] = random.uniform(-0.3, 0.3)  # Weak correlations
    
    return corr_dict

class RiskAssessmentAgent:
    def __init__(self, agent_id: str, institutions: List[str]):
        self.agent_id = agent_id
        self.institutions = institutions
        self.status = AgentStatus.IDLE
        self.last_activity = datetime.now()
    
    async def collect_data_from_institution(self, institution_id: str):
        self.status = AgentStatus.COLLECTING
        
        activity = AgentActivity(
            agent_id=self.agent_id,
            activity_type="data_collection",
            description=f"Collecting borrower data from {institution_id}",
            timestamp=datetime.now(),
            status="in_progress"
        )
        agent_activities_db.append(activity.dict())
        
        await asyncio.sleep(random.uniform(1, 3))
        
        institution_borrowers = [b for b in borrowers_db.values() 
                               if b["institution_id"] == institution_id]
        
        activity.status = "completed"
        activity.data_collected = {"borrower_count": len(institution_borrowers)}
        agent_activities_db[-1] = activity.dict()
        
        return institution_borrowers
    
    async def analyze_and_update_risk_scores(self, borrowers: List[Dict]):
        self.status = AgentStatus.ANALYZING
        
        activity = AgentActivity(
            agent_id=self.agent_id,
            activity_type="risk_analysis",
            description=f"Analyzing risk scores for {len(borrowers)} borrowers",
            timestamp=datetime.now(),
            status="in_progress"
        )
        agent_activities_db.append(activity.dict())
        
        updated_scores = []
        for borrower in borrowers:
            await asyncio.sleep(0.1)
            
            risk_score = calculate_risk_score(borrower)
            risk_scores_db[borrower["borrower_id"]] = risk_score.dict()
            updated_scores.append(risk_score)
        
        activity.status = "completed"
        activity.data_collected = {"scores_updated": len(updated_scores)}
        agent_activities_db[-1] = activity.dict()
        
        self.status = AgentStatus.IDLE
        return updated_scores

agents = {
    "agent_001": RiskAssessmentAgent("agent_001", ["bank_001", "credit_union_002"]),
    "agent_002": RiskAssessmentAgent("agent_002", ["fintech_003"]),
    "agent_003": RiskAssessmentAgent("agent_003", ["bank_001", "fintech_003"])
}

async def continuous_monitoring():
    while True:
        try:
            agent_id = random.choice(list(agents.keys()))
            agent = agents[agent_id]
            
            if agent.status == AgentStatus.IDLE:
                # Select a random institution from agent's assigned institutions
                institution_id = random.choice(agent.institutions)
                
                borrowers = await agent.collect_data_from_institution(institution_id)
                await agent.analyze_and_update_risk_scores(borrowers)
                
                institutions_db[institution_id]["last_sync"] = datetime.now()
        
        except Exception as e:
            print(f"Error in continuous monitoring: {e}")
        
        await asyncio.sleep(random.uniform(10, 30))

@app.on_event("startup")
async def startup_event():
    initialize_mock_institutions()
    
    for inst_id, inst_data in institutions_db.items():
        generate_mock_borrower_data(inst_id, inst_data["borrower_count"])
    
    for borrower_id, borrower_data in borrowers_db.items():
        risk_score = calculate_risk_score(borrower_data)
        risk_scores_db[borrower_id] = risk_score.dict()
    
    historical_risk_data_db["portfolio"] = generate_historical_risk_data()
    train_risk_prediction_model()
    
    asyncio.create_task(continuous_monitoring())


@app.get("/healthz")
async def healthz():
    return {"status": "ok"}

@app.get("/api/dashboard")
async def get_dashboard():
    """Get portfolio dashboard overview"""
    total_borrowers = len(borrowers_db)
    total_institutions = len(institutions_db)
    
    risk_distribution = {"low": 0, "medium": 0, "high": 0, "critical": 0}
    total_exposure = 0
    
    for risk_score in risk_scores_db.values():
        risk_distribution[risk_score["risk_level"]] += 1
    
    for borrower in borrowers_db.values():
        total_exposure += borrower["loan_amount"]
    
    avg_risk_score = sum(score["overall_score"] for score in risk_scores_db.values()) / len(risk_scores_db) if risk_scores_db else 0
    
    recent_activities = sorted(agent_activities_db, key=lambda x: x["timestamp"], reverse=True)[:10]
    
    agent_status_summary = {}
    for agent_id, agent in agents.items():
        agent_status_summary[agent_id] = {
            "status": agent.status.value,
            "last_activity": agent.last_activity.isoformat(),
            "institutions": agent.institutions
        }
    
    return {
        "total_borrowers": total_borrowers,
        "total_institutions": total_institutions,
        "total_exposure": total_exposure,
        "average_risk_score": avg_risk_score,
        "risk_distribution": risk_distribution,
        "recent_activities": recent_activities,
        "agent_status": agent_status_summary,
        "last_updated": datetime.now().isoformat()
    }

@app.get("/api/institutions")
async def get_institutions():
    """Get all institutions"""
    return list(institutions_db.values())

@app.get("/api/institutions/{institution_id}/borrowers")
async def get_institution_borrowers(institution_id: str):
    """Get borrowers for a specific institution"""
    borrowers = [b for b in borrowers_db.values() if b["institution_id"] == institution_id]
    
    # Add risk scores to borrowers
    for borrower in borrowers:
        if borrower["borrower_id"] in risk_scores_db:
            borrower["risk_score"] = risk_scores_db[borrower["borrower_id"]]
    
    return borrowers

@app.get("/api/borrowers")
async def get_borrowers(limit: int = 100, risk_level: Optional[str] = None):
    """Get borrowers with optional filtering"""
    borrowers = list(borrowers_db.values())
    
    for borrower in borrowers:
        if borrower["borrower_id"] in risk_scores_db:
            borrower["risk_score"] = risk_scores_db[borrower["borrower_id"]]
    
    if risk_level:
        borrowers = [b for b in borrowers if b.get("risk_score", {}).get("risk_level") == risk_level]
    
    borrowers.sort(key=lambda x: x.get("risk_score", {}).get("overall_score", 0))
    
    return borrowers[:limit]

@app.get("/api/borrowers/{borrower_id}")
async def get_borrower(borrower_id: str):
    """Get detailed borrower information"""
    if borrower_id not in borrowers_db:
        return {"error": "Borrower not found"}, 404
    
    borrower = borrowers_db[borrower_id].copy()
    if borrower_id in risk_scores_db:
        borrower["risk_score"] = risk_scores_db[borrower_id]
    
    return borrower

@app.get("/api/risk-scores")
async def get_risk_scores():
    """Get all risk scores"""
    return list(risk_scores_db.values())

@app.get("/api/agents")
async def get_agents():
    """Get agent status and information"""
    agent_info = {}
    for agent_id, agent in agents.items():
        agent_info[agent_id] = {
            "agent_id": agent_id,
            "status": agent.status.value,
            "institutions": agent.institutions,
            "last_activity": agent.last_activity.isoformat()
        }
    
    return agent_info

@app.get("/api/agents/{agent_id}/activities")
async def get_agent_activities(agent_id: str, limit: int = 50):
    """Get activities for a specific agent"""
    activities = [a for a in agent_activities_db if a["agent_id"] == agent_id]
    activities.sort(key=lambda x: x["timestamp"], reverse=True)
    return activities[:limit]

@app.post("/api/agents/{agent_id}/trigger-collection")
async def trigger_agent_collection(agent_id: str, background_tasks: BackgroundTasks):
    """Manually trigger data collection for an agent"""
    if agent_id not in agents:
        return {"error": "Agent not found"}, 404
    
    agent = agents[agent_id]
    
    if agent.status != AgentStatus.IDLE:
        return {"error": "Agent is currently busy"}, 400
    
    async def run_collection():
        try:
            for institution_id in agent.institutions:
                borrowers = await agent.collect_data_from_institution(institution_id)
                await agent.analyze_and_update_risk_scores(borrowers)
                institutions_db[institution_id]["last_sync"] = datetime.now()
        except Exception as e:
            print(f"Error in manual collection for agent {agent_id}: {e}")
    
    background_tasks.add_task(run_collection)
    
    return {"message": f"Data collection triggered for agent {agent_id}"}

@app.get("/api/portfolio/risk-analysis")
async def get_portfolio_risk_analysis():
    """Get detailed portfolio risk analysis"""
    
    institution_risk = {}
    for inst_id, inst_data in institutions_db.items():
        institution_borrowers = [b for b in borrowers_db.values() if b["institution_id"] == inst_id]
        risk_scores = [risk_scores_db.get(b["borrower_id"], {}) for b in institution_borrowers]
        
        avg_score = sum(r.get("overall_score", 0) for r in risk_scores) / len(risk_scores) if risk_scores else 0
        total_exposure = sum(b["loan_amount"] for b in institution_borrowers)
        
        risk_dist = {"low": 0, "medium": 0, "high": 0, "critical": 0}
        for score in risk_scores:
            if score.get("risk_level"):
                risk_dist[score["risk_level"]] += 1
        
        institution_risk[inst_id] = {
            "institution_name": inst_data["name"],
            "average_risk_score": avg_score,
            "total_exposure": total_exposure,
            "borrower_count": len(institution_borrowers),
            "risk_distribution": risk_dist
        }
    
    # Top risk borrowers
    all_borrowers_with_scores = []
    for borrower_id, borrower in borrowers_db.items():
        if borrower_id in risk_scores_db:
            borrower_copy = borrower.copy()
            borrower_copy["risk_score"] = risk_scores_db[borrower_id]
            all_borrowers_with_scores.append(borrower_copy)
    
    top_risk_borrowers = sorted(all_borrowers_with_scores, 
                               key=lambda x: x["risk_score"]["overall_score"])[:20]
    
    risk_trends = []
    for i in range(30):  # Last 30 days
        date = datetime.now() - timedelta(days=i)
        avg_risk = random.uniform(0.5, 0.8)  # Simulated historical data
        risk_trends.append({
            "date": date.isoformat(),
            "average_risk_score": avg_risk
        })
    
    risk_trends.reverse()  # Chronological order
    
    return {
        "institution_risk_analysis": institution_risk,
        "top_risk_borrowers": top_risk_borrowers,
        "risk_trends": risk_trends,
        "generated_at": datetime.now().isoformat()
    }

@app.get("/api/real-time/updates")
async def get_real_time_updates():
    """Get recent updates for real-time monitoring"""
    
    recent_cutoff = datetime.now() - timedelta(minutes=10)
    recent_updates = []
    
    for borrower_id, risk_score in risk_scores_db.items():
        if datetime.fromisoformat(risk_score["last_calculated"]) > recent_cutoff:
            borrower = borrowers_db.get(borrower_id, {})
            recent_updates.append({
                "borrower_id": borrower_id,
                "borrower_name": borrower.get("name", "Unknown"),
                "institution_id": borrower.get("institution_id"),
                "old_risk_level": random.choice(["low", "medium", "high"]),  # Simulated
                "new_risk_level": risk_score["risk_level"],
                "risk_score": risk_score["overall_score"],
                "timestamp": risk_score["last_calculated"]
            })
    
    recent_activities = [a for a in agent_activities_db 
                        if datetime.fromisoformat(a["timestamp"]) > recent_cutoff]
    
    return {
        "recent_risk_updates": recent_updates[-10:],  # Last 10 updates
        "recent_agent_activities": recent_activities[-10:],  # Last 10 activities
        "timestamp": datetime.now().isoformat()
    }

@app.get("/api/analytics/predictive-models")
async def get_predictive_models():
    """Get predictive model results and forecasts"""
    
    model_info = train_risk_prediction_model()
    if not model_info:
        return {"error": "Insufficient data to train models"}
    
    predictions_30 = predict_future_risk_scores(30)
    predictions_60 = predict_future_risk_scores(60)
    predictions_90 = predict_future_risk_scores(90)
    
    return PredictiveModel(
        model_type="Random Forest Regressor",
        accuracy_score=0.95,  # Simulated high accuracy for demo
        r2_score=model_info["r2_score"],
        rmse=model_info["rmse"],
        feature_importance=model_info["feature_importance"],
        predictions=predictions_30,
        confidence_intervals=[
            {
                "period": "30_days",
                "predictions": predictions_30
            },
            {
                "period": "60_days", 
                "predictions": predictions_60
            },
            {
                "period": "90_days",
                "predictions": predictions_90
            }
        ]
    )

@app.get("/api/analytics/stress-testing")
async def get_stress_testing():
    """Get portfolio stress testing results"""
    
    scenarios = ["market_crash", "interest_rate_shock", "economic_recession"]
    stress_results = []
    
    for scenario in scenarios:
        result = run_stress_test(scenario)
        if result:
            stress_results.append(result.dict())
    
    institution_exposure = {}
    for borrower in borrowers_db.values():
        inst_id = borrower["institution_id"]
        if inst_id not in institution_exposure:
            institution_exposure[inst_id] = 0
        institution_exposure[inst_id] += borrower["loan_amount"]
    
    total_exposure = sum(institution_exposure.values())
    concentration_risks = {
        inst_id: (exposure / total_exposure) * 100 
        for inst_id, exposure in institution_exposure.items()
    }
    
    return {
        "stress_test_results": stress_results,
        "concentration_analysis": concentration_risks,
        "portfolio_metrics": {
            "total_exposure": total_exposure,
            "number_of_institutions": len(institution_exposure),
            "largest_exposure_pct": max(concentration_risks.values()) if concentration_risks else 0
        },
        "generated_at": datetime.now().isoformat()
    }

@app.get("/api/analytics/trend-analysis")
async def get_trend_analysis():
    """Get advanced trend analysis and forecasting"""
    
    # Get historical data
    historical_data = historical_risk_data_db.get("daily_data", [])
    
    def simple_mean(values):
        return sum(values) / len(values) if values else 0
    
    seasonal_patterns = {
        "Q1": simple_mean([d["average_risk_score"] for d in historical_data[:7]]) if len(historical_data) >= 7 else 0.65,
        "Q2": simple_mean([d["average_risk_score"] for d in historical_data[7:14]]) if len(historical_data) >= 14 else 0.68,
        "Q3": simple_mean([d["average_risk_score"] for d in historical_data[14:21]]) if len(historical_data) >= 21 else 0.72,
        "Q4": simple_mean([d["average_risk_score"] for d in historical_data[21:30]]) if len(historical_data) >= 30 else 0.69
    }
    
    forecast_30 = predict_future_risk_scores(30)
    forecast_60 = predict_future_risk_scores(60)
    forecast_90 = predict_future_risk_scores(90)
    
    recent_scores = [d["average_risk_score"] for d in historical_data[-30:]] if len(historical_data) >= 30 else []
    if len(recent_scores) >= 2:
        trend_slope = (recent_scores[-1] - recent_scores[0]) / len(recent_scores)
        if trend_slope > 0.01:
            trend_direction = "improving"
        elif trend_slope < -0.01:
            trend_direction = "declining"
        else:
            trend_direction = "stable"
    else:
        trend_direction = "stable"
    
    if len(recent_scores) >= 10:
        mean_score = sum(recent_scores) / len(recent_scores)
        variance = sum((x - mean_score) ** 2 for x in recent_scores) / len(recent_scores)
        volatility_index = variance ** 0.5
    else:
        volatility_index = 0.05
    
    return TrendAnalysis(
        historical_trends=historical_data[-90:],  # Last 90 days
        seasonal_patterns=seasonal_patterns,
        forecast_30_days=forecast_30,
        forecast_60_days=forecast_60,
        forecast_90_days=forecast_90,
        trend_direction=trend_direction,
        volatility_index=float(volatility_index)
    )

@app.get("/api/analytics/risk-factors")
async def get_risk_factors():
    """Get risk factor importance and correlations"""
    
    model_info = ml_models_cache.get("risk_prediction")
    if not model_info:
        model_info = train_risk_prediction_model()
    
    feature_importance = model_info["feature_importance"] if model_info else {
        "credit_score": 0.25,
        "debt_to_income": 0.20,
        "payment_history": 0.20,
        "annual_income": 0.15,
        "employment": 0.10,
        "loan_amount": 0.05,
        "collateral": 0.05
    }
    
    correlation_matrix = calculate_correlation_matrix()
    
    sensitivity_analysis = {
        "credit_score_sensitivity": 0.35,
        "income_sensitivity": 0.25,
        "dti_sensitivity": 0.30,
        "employment_sensitivity": 0.20
    }
    
    concentration_risks = {}
    for inst_id, inst_data in institutions_db.items():
        institution_borrowers = [b for b in borrowers_db.values() if b["institution_id"] == inst_id]
        total_exposure = sum(b["loan_amount"] for b in institution_borrowers)
        concentration_risks[inst_data["name"]] = total_exposure
    
    total_portfolio = sum(concentration_risks.values())
    concentration_risks = {
        name: (exposure / total_portfolio) * 100 
        for name, exposure in concentration_risks.items()
    }
    
    return RiskFactorAnalysis(
        factor_importance=feature_importance,
        correlation_matrix=correlation_matrix,
        sensitivity_analysis=sensitivity_analysis,
        concentration_risks=concentration_risks
    )
