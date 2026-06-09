create table payments (
    id varchar(36) primary key,
    amount_cents bigint not null check (amount_cents > 0),
    currency char(3) not null,
    merchant_id varchar(64) not null,
    customer_id varchar(64) not null,
    status varchar(32) not null default 'CREATED',
    metadata jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table idempotency_keys (
    idempotency_key varchar(128) primary key,
    request_hash varchar(64) not null,
    http_status int not null,
    response_body jsonb not null,
    payment_id varchar(36) not null references payments(id),
    created_at timestamptz not null default now(),
    expires_at timestamptz
);

create index idx_idempotency_keys_expires_at on idempotency_keys(expires_at);