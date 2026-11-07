-- when we last told this user "someone tried to register with your email"; limits those emails to one per hour
ALTER TABLE users ADD COLUMN existing_account_email_sent_at TIMESTAMPTZ;
