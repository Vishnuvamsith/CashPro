CREATE TABLE payments (
                          payment_id UUID PRIMARY KEY,
                          client_id VARCHAR(50) NOT NULL,
                          debit_account VARCHAR(50) NOT NULL,
                          credit_account VARCHAR(50) NOT NULL,
                          amount NUMERIC(18,2) NOT NULL,
                          currency VARCHAR(3) NOT NULL,
                          status VARCHAR(30) NOT NULL,
                          idempotency_key VARCHAR(100) UNIQUE NOT NULL,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL
);