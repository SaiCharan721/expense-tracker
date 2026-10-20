import { MongoClient } from 'mongodb';
import { createApp } from './app.js';
import { loadConfig } from './config.js';
import { createExpenseRepository } from './expenseRepository.js';

const config = loadConfig();
const client = new MongoClient(config.mongoUri);

await client.connect();
const repo = createExpenseRepository(client.db());
const app = createApp({ repo, jwtSecret: config.jwtSecret, corsOrigins: config.corsOrigins });

const server = app.listen(config.port, () => {
  console.log(`report-service listening on port ${config.port}`);
});

async function shutdown(signal) {
  console.log(`${signal} received, shutting down`);
  server.close();
  await client.close();
  process.exit(0);
}

process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));
