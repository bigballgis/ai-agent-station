-- AI Agent Station Database Initialization
-- This script runs on first container start

-- Enable pgvector extension (already included in pgvector/pgvector image)
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
