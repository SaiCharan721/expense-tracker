import express from 'express';
import { requireAuth } from './auth.js';
import { buildTrend, formatMonth, lastMonths, monthRange, parseMonth, toCsv } from './reports.js';

const MAX_TREND_MONTHS = 12;

export function createApp({ repo, jwtSecret, corsOrigins = [], now = () => new Date() }) {
  const app = express();
  app.disable('x-powered-by');

  app.use((req, res, next) => {
    const origin = req.get('origin');
    if (origin && corsOrigins.includes(origin)) {
      res.set('Access-Control-Allow-Origin', origin);
      res.set('Access-Control-Allow-Headers', 'Authorization, Content-Type');
      res.set('Access-Control-Expose-Headers', 'Content-Disposition');
      res.set('Vary', 'Origin');
    }
    if (req.method === 'OPTIONS') return res.sendStatus(204);
    return next();
  });

  app.get('/health', (req, res) => res.json({ status: 'UP' }));

  const reports = express.Router();
  reports.use(requireAuth(jwtSecret));

  reports.get('/monthly.csv', async (req, res, next) => {
    try {
      const month = req.query.month ? parseMonth(req.query.month) : lastMonths(1, now())[0];
      if (!month) return badRequest(res, 'month must look like 2026-10');

      const { from, to } = monthRange(month);
      const expenses = await repo.findByUserBetween(req.user.id, from, to);
      res.set('Content-Type', 'text/csv; charset=utf-8');
      res.set('Content-Disposition', `attachment; filename="expenses-${formatMonth(month)}.csv"`);
      return res.send(toCsv(expenses));
    } catch (err) {
      return next(err);
    }
  });

  reports.get('/trend', async (req, res, next) => {
    try {
      const count = req.query.months === undefined ? 6 : Number(req.query.months);
      if (!Number.isInteger(count) || count < 1 || count > MAX_TREND_MONTHS) {
        return badRequest(res, `months must be a whole number from 1 to ${MAX_TREND_MONTHS}`);
      }
      const months = lastMonths(count, now());
      const from = monthRange(months[0]).from;
      const to = monthRange(months[months.length - 1]).to;
      const expenses = await repo.findByUserBetween(req.user.id, from, to);
      return res.json({ months: buildTrend(expenses, months) });
    } catch (err) {
      return next(err);
    }
  });

  app.use('/reports', reports);

  app.use((req, res) => res.status(404).json({ title: 'Not Found', detail: `No route for ${req.path}` }));

  // eslint-disable-next-line no-unused-vars
  app.use((err, req, res, next) => {
    console.error(err);
    res.status(500).json({ title: 'Internal Server Error', detail: 'Could not build the report' });
  });

  return app;
}

function badRequest(res, detail) {
  return res.status(400).json({ title: 'Bad Request', detail });
}
