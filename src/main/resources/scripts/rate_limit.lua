-- KEYS[1]: Redis Key for the API Tenant (e.g., "tokenshield:rate_limit:tenant_123")
-- ARGV[1]: Current Epoch Milliseconds
-- ARGV[2]: Sliding Window Duration in Milliseconds (e.g., 60000 for 1 min)
-- ARGV[3]: Maximum Allowed Tokens in Window
-- ARGV[4]: Estimated Tokens for Current Request
-- ARGV[5]: Unique Request Identifier (UUID)

local rate_limit_key = KEYS[1]
local now = tonumber(ARGV[1])
local window_start = now - tonumber(ARGV[2])
local max_tokens = tonumber(ARGV[3])
local requested_tokens = tonumber(ARGV[4])
local request_id = ARGV[5]

-- 1. Remove expired requests outside the sliding window
redis.call('ZREMRANGEBYSCORE', rate_limit_key, '-inf', window_start)

-- 2. Sum tokens consumed in the current active window
local current_usage = 0
local entries = redis.call('ZRANGE', rate_limit_key, 0, -1, 'WITHSCORES')

for i = 1, #entries, 2 do
    -- Member format stored as "UUID:token_count"
    local member = entries[i]
    local colon_idx = string.find(member, ":")
    if colon_idx then
        local tokens = tonumber(string.sub(member, colon_idx + 1))
        if tokens then
            current_usage = current_usage + tokens
        end
    end
end

-- 3. Reject request if adding requested tokens exceeds budget
if (current_usage + requested_tokens) > max_tokens then
    return {0, current_usage, max_tokens - current_usage} -- [Allowed (0=false), Current Usage, Remaining Tokens]
end

-- 4. Record current request with score = timestamp
local new_member = request_id .. ":" .. tostring(requested_tokens)
redis.call('ZADD', rate_limit_key, now, new_member)

-- 5. Set TTL to auto-clean key when idle
redis.call('PEXPIRE', rate_limit_key, tonumber(ARGV[2]))

return {1, current_usage + requested_tokens, max_tokens - (current_usage + requested_tokens)} -- [Allowed (1=true), Updated Usage, Remaining Tokens]