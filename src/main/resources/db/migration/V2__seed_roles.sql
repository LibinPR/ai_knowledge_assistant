INSERT INTO roles(name , description)
VALUES
    ('USER' , 'Default application user'),
    ('ADMIN' , 'System administrator')
ON CONFLICT (name) DO NOTHING;