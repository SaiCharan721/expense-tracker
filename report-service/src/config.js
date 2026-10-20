// Same dev secret as expense-api. Both services must share JWT_SECRET.
const DEV_SECRET = 'ZGV2LW9ubHktc2VjcmV0LWNoYW5nZS1tZS1pbi1wcm9kdWN0aW9uLTEyMzQ1Njc4OTA=';

export function loadConfig(env = process.env) {
  return {
    port: Number(env.PORT ?? 4000),
    mongoUri: env.MONGODB_URI ?? 'mongodb://localhost:27017/expense_tracker',
    // expense-api treats the secret as base64, so decode it the same way here
    jwtSecret: Buffer.from(env.JWT_SECRET ?? DEV_SECRET, 'base64'),
    corsOrigins: (env.CORS_ORIGINS ?? 'http://localhost:5173,http://localhost:3000').split(',').map((s) => s.trim()),
  };
}
