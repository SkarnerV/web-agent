-- V9: Remove hardcoded stub builtin models
-- The three models (GPT-4o, GPT-4o Mini, Claude 3.5 Sonnet) inserted in V6
-- were placeholder stubs. Real models should be configured by the administrator
-- via the Model Management UI or API.

DELETE FROM builtin_models;
