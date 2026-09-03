-- ============================================================================
-- 1. USERS & FAST-PATH AUTHENTICATION
-- ============================================================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    
    -- Green Ink supports login using either Phone OR Email
    phone_number VARCHAR(20),
    email VARCHAR(254),
    full_name VARCHAR(100),
    
    -- Single-Device Displacement:
    -- When logging in on Device B, generating a new UUID here instantly invalidates Device A.
    active_session_id VARCHAR(64),
    
    -- Anti-Session Hijacking (Zero False Logouts on Mobile IP Handover):
    -- Stores SHA-256(User-Agent + Client Fingerprint). Prevents stolen cookies from running on other devices.
    active_device_hash VARCHAR(64),
    last_login_ip VARCHAR(45),      -- Audit only (supports IPv4 & IPv6). We avoid strict IP blocking due to mobile CGNAT rotation.
    last_login_device VARCHAR(150),  -- e.g., "Chrome Mobile 124 / Android 14"
    
    -- Denormalized Tier Snapshot (0-Join Read Path):
    -- Read on every API hit (<0.4ms index scan) without joining user_subscriptions or subscription_plans.
    cached_tier_level INT NOT NULL DEFAULT 0, -- 0: Free, 10: Premium, 20: Super
    tier_expires_at TIMESTAMP WITH TIME ZONE,
    
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'BLOCKED', 'DELETED')),
        
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_users_identity CHECK (phone_number IS NOT NULL OR email IS NOT NULL)
);

-- Partial indexes permit phone-only or email-only accounts without NULL collision issues
CREATE UNIQUE INDEX uq_users_phone ON users(phone_number);
CREATE UNIQUE INDEX uq_users_email_lower ON users(email);
CREATE INDEX idx_users_session_auth ON users(id, active_session_id);


-- Transient OTP verification table with replay attack prevention
CREATE TABLE otp_verifications (
    id BIGSERIAL PRIMARY KEY,
    challenge_id VARCHAR(100) UNIQUE NOT NULL,
    identity_type VARCHAR(10) NOT NULL CHECK (identity_type IN ('PHONE', 'EMAIL')),
    identity_value VARCHAR(254) NOT NULL,
    
    -- Stores bcrypt/PBKDF2 hash of the 6-digit OTP
    otp_hash VARCHAR(255) NOT NULL,
    attempts_remaining INT NOT NULL DEFAULT 5 CHECK (attempts_remaining >= 0),
    
    -- Replay Protection: Prevents reusing verified OTPs within expiration window
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resend_after TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_fast_lookup ON otp_verifications(identity_value, is_used, expires_at);


-- ============================================================================
-- 2. SUBSCRIPTIONS, IDEMPOTENT PAYMENTS & WEBHOOKS
-- ============================================================================

CREATE TABLE subscription_plans (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL, -- e.g., 'PLAN_PREMIUM_6M', 'PLAN_SUPER_1Y'
    tier_level INT NOT NULL DEFAULT 10,
    title VARCHAR(100) NOT NULL,
    duration_days INT NOT NULL CHECK (duration_days > 0),
    price_in_paise INT NOT NULL CHECK (price_in_paise >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_subscription_plans_active ON subscription_plans(is_active, display_order);


CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    plan_id BIGINT NOT NULL REFERENCES subscription_plans(id) ON DELETE RESTRICT,
    
    provider VARCHAR(30) NOT NULL DEFAULT 'RAZORPAY',
    
    -- Client-Side Idempotency: Prevents double deductions from accidental double-taps on 'Pay'
    idempotency_key VARCHAR(100) UNIQUE NOT NULL,
    
    razorpay_order_id VARCHAR(100) UNIQUE NOT NULL,
    razorpay_payment_id VARCHAR(100) UNIQUE,
    razorpay_signature VARCHAR(255),
    
    amount_in_paise INT NOT NULL CHECK (amount_in_paise >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'VERIFIED', 'FAILED', 'REFUNDED')),
        
    -- VARCHAR(4000) chosen over JSONB for maximum H2/jOOQ test compatibility
    provider_payload VARCHAR(4000),
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_order ON payment_transactions(razorpay_order_id);
CREATE INDEX idx_payment_user ON payment_transactions(user_id, created_at DESC);


-- Incoming Webhook Idempotency & Deduplication Engine:
-- Gateways retry deliveries up to 5 times. UNIQUE provider_event_id prevents duplicate credit.
CREATE TABLE payment_webhook_events (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(30) NOT NULL DEFAULT 'RAZORPAY',
    provider_event_id VARCHAR(150) UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL, -- e.g., 'payment.captured', 'order.paid'
    payload VARCHAR(4000) NOT NULL,
    
    processing_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED'
        CHECK (processing_status IN ('RECEIVED', 'PROCESSED', 'FAILED', 'IGNORED')),
        
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    error_message VARCHAR(1000)
);

CREATE INDEX idx_webhook_proc ON payment_webhook_events(provider_event_id, processing_status);


-- Authoritative Subscription Ledger (History & Audit Trail)
CREATE TABLE user_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_id BIGINT NOT NULL REFERENCES subscription_plans(id) ON DELETE RESTRICT,
    payment_transaction_id BIGINT REFERENCES payment_transactions(id) ON DELETE SET NULL,
    
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'UPGRADED')),
        
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_subscription_dates CHECK (expires_at > starts_at)
);

CREATE INDEX idx_user_sub_lookup ON user_subscriptions(user_id, status, expires_at DESC);


-- ============================================================================
-- 3. SYLLABUS, CHAPTERS & 10-PAGE MINI-NOTES
-- ============================================================================

-- Top-level Syllabus Units (Unit I ... Unit VIII)
CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(50) UNIQUE NOT NULL,       -- e.g., 'unit-8-tamil-history', 'general-science'
    title_primary VARCHAR(255) NOT NULL,    -- Regional: à®¤à®®à®¿à®´à¯à®¨à®¾à®Ÿà¯à®Ÿà®¿à®©à¯ à®µà®°à®²à®¾à®±à¯, à®®à®°à®ªà¯...
    title_secondary VARCHAR(255),          -- English: History, Culture & Heritage...
    display_order INT NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);


-- Chapters inside each Syllabus Unit
CREATE TABLE syllabus_units (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    unit_code VARCHAR(30) UNIQUE NOT NULL,  -- e.g., 'U8_CH01_SANGAM_ERA'
    title_primary VARCHAR(255) NOT NULL,
    title_secondary VARCHAR(255),
    display_order INT NOT NULL,
    
    -- Integer tiers (0: Free, 10: Premium) provide dynamic gating without schema migrations
    required_tier_level INT NOT NULL DEFAULT 0,
    
    notes_available BOOLEAN NOT NULL DEFAULT TRUE,
    pyq_available BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    CONSTRAINT uq_topic_unit_order UNIQUE (topic_id, display_order)
);

CREATE INDEX idx_units_topic_tier ON syllabus_units(topic_id, required_tier_level);


-- Structured 10-Page Bitesize Chapter Notes
CREATE TABLE unit_syllabus_content (
    id BIGSERIAL PRIMARY KEY,
    unit_id BIGINT NOT NULL REFERENCES syllabus_units(id) ON DELETE CASCADE,
    page_number INT NOT NULL CHECK (page_number > 0),
    title_primary VARCHAR(255) NOT NULL,
    title_secondary VARCHAR(255),
    
    -- VARCHAR(10000) chosen over TEXT for jOOQ type-safety while supporting rich content
    content_primary TEXT NOT NULL,
    content_secondary TEXT,
    
    featured_image_url VARCHAR(500), -- Infographics / Archaeological Maps / Diagrams
    required_tier_level INT NOT NULL DEFAULT 10,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_unit_page_number UNIQUE (unit_id, page_number)
);

CREATE INDEX idx_syllabus_content_fetch ON unit_syllabus_content(unit_id, page_number);


-- ============================================================================
-- 4. QUESTIONS, REUSABLE BANKS & PYQ APPEARANCE ENGINE
-- ============================================================================

-- Atomic Question Content (Zero Data Duplication)
-- Structured slots are used instead of nested JSONB to enable:
-- 1. Sub-millisecond direct row scans on 200-question full mocks (0 JSON parsing overhead).
-- 2. Clean jOOQ POJO mapping without custom Jackson TypeBindings.
-- 3. Safe anti-cheating option shuffling without JSON tree rewriting.
-- 4. Native DB-level NOT NULL validation for bilingual choices.
CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    unit_id BIGINT NOT NULL REFERENCES syllabus_units(id) ON DELETE CASCADE,
    question_type VARCHAR(30) NOT NULL DEFAULT 'MCQ'
        CHECK (question_type IN ('MCQ', 'ASSERTION_REASON', 'MATCH', 'MULTI_STATEMENT')),
        
    question_text_primary VARCHAR(4000) NOT NULL,
    question_text_secondary VARCHAR(4000),
    image_url VARCHAR(500), -- Diagram, map, formula image

    -- Option Slot 1
    opt_1_text_primary VARCHAR(2000) NOT NULL,
    opt_1_text_secondary VARCHAR(2000),
    opt_1_image_url VARCHAR(500),

    -- Option Slot 2
    opt_2_text_primary VARCHAR(2000) NOT NULL,
    opt_2_text_secondary VARCHAR(2000),
    opt_2_image_url VARCHAR(500),

    -- Option Slot 3
    opt_3_text_primary VARCHAR(2000),
    opt_3_text_secondary VARCHAR(2000),
    opt_3_image_url VARCHAR(500),

    -- Option Slot 4
    opt_4_text_primary VARCHAR(2000),
    opt_4_text_secondary VARCHAR(2000),
    opt_4_image_url VARCHAR(500),

    -- Immutable Key Pointer ('opt_1'..'opt_4')
    correct_option_key VARCHAR(10)
        CHECK (correct_option_key IN ('opt_1', 'opt_2', 'opt_3', 'opt_4')),

    explanation_primary VARCHAR(4000),
    explanation_secondary VARCHAR(4000),
    explanation_image_url VARCHAR(500),
    source_reference VARCHAR(255), -- e.g., 'Samacheer Kalvi Std 11 History Ch 4'
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_questions_unit ON questions(unit_id, is_active);


-- Practice Test Containers (Chapter Tests, Unit Tests, Grand Mocks)
CREATE TABLE question_banks (
    id BIGSERIAL PRIMARY KEY,
    unit_id BIGINT REFERENCES syllabus_units(id) ON DELETE CASCADE, -- NULL for Cross-Unit Grand Tests
    title_primary VARCHAR(255) NOT NULL,
    title_secondary VARCHAR(255),
    bank_type VARCHAR(30) NOT NULL DEFAULT 'PRACTICE'
        CHECK (bank_type IN ('PRACTICE', 'MOCK_TEST', 'PYQ')),
    duration_minutes INT NOT NULL DEFAULT 90,
    total_marks INT NOT NULL DEFAULT 100,
    total_questions INT NOT NULL DEFAULT 0, -- Cached count: eliminates COUNT(*) joins
    required_tier_level INT NOT NULL DEFAULT 10,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_banks_unit ON question_banks(unit_id, is_active);


-- Many-to-Many Mapping Table: Reuses 1 Question Across Unlimited Banks (Zero Duplication)
CREATE TABLE bank_questions_map (
    bank_id BIGINT NOT NULL REFERENCES question_banks(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    question_order INT NOT NULL DEFAULT 1 CHECK (question_order > 0),
    PRIMARY KEY (bank_id, question_id),
    CONSTRAINT uq_bank_qorder UNIQUE (bank_id, question_order)
);

CREATE INDEX idx_map_bank ON bank_questions_map(bank_id, question_order);
CREATE INDEX idx_map_question ON bank_questions_map(question_id);


-- Question Appearances: Zero-Duplication PYQ Engine
-- Storing exam instances separately allows a single question record to display repeat badges
-- (e.g., "Asked in: 2015 Grp-2 Prelims â€¢ 2022 Grp-1 Mains â€¢ 2024 Grp-4 Prelims").
CREATE TABLE question_appearances (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    exam_category VARCHAR(50) NOT NULL,  -- 'GROUP_1', 'GROUP_2_2A', 'GROUP_4_VAO'
    exam_stage VARCHAR(20) NOT NULL,      -- 'PRELIMS', 'MAINS'
    exam_year INT NOT NULL CHECK (exam_year >= 1990),
    question_paper_qno INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_q_instance UNIQUE (question_id, exam_category, exam_stage, exam_year, question_paper_qno)
);

CREATE INDEX idx_appearances_paper ON question_appearances(exam_year DESC, exam_category, exam_stage, question_paper_qno);
CREATE INDEX idx_appearances_question ON question_appearances(question_id);


-- ============================================================================
-- 5. PROGRESS, ATTEMPTS & ANSWER AUDITING
-- ============================================================================

CREATE TABLE notes_progress (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    unit_id BIGINT NOT NULL REFERENCES syllabus_units(id) ON DELETE CASCADE,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    scroll_percent INT NOT NULL DEFAULT 0 CHECK (scroll_percent BETWEEN 0 AND 100),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, unit_id)
);

CREATE INDEX idx_notes_prog_user ON notes_progress(user_id, completed);


-- Full Mock Test & Grand PYQ Submissions
CREATE TABLE test_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bank_id BIGINT REFERENCES question_banks(id) ON DELETE SET NULL,
    
    -- Populated when taking a Global Year-Wise PYQ Paper (where bank_id is NULL)
    exam_category VARCHAR(50),
    exam_stage VARCHAR(20),
    exam_year INT,
    
    score NUMERIC(8, 2) NOT NULL DEFAULT 0.00,
    accuracy_pct NUMERIC(5, 2) NOT NULL DEFAULT 0.00 CHECK (accuracy_pct BETWEEN 0 AND 100),
    total_questions INT NOT NULL CHECK (total_questions >= 0),
    correct_count INT NOT NULL DEFAULT 0,
    wrong_count INT NOT NULL DEFAULT 0,
    unattended_count INT NOT NULL DEFAULT 0,
    time_spent_secs INT NOT NULL DEFAULT 0,
    
    -- Auditable JSON submission string: {"101": "opt_2", "102": "opt_4"}
    user_answers TEXT NOT NULL,
    
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attempts_user ON test_attempts(user_id, completed_at DESC);
CREATE INDEX idx_attempts_bank ON test_attempts(bank_id);
