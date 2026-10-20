import jwt from 'jsonwebtoken';

// jjwt picks HS256/384/512 based on the key length, so accept the whole HMAC family
const ALGORITHMS = ['HS256', 'HS384', 'HS512'];

export function requireAuth(secret) {
  return (req, res, next) => {
    const header = req.get('authorization') ?? '';
    if (!header.startsWith('Bearer ')) {
      return res.status(401).json({ title: 'Unauthorized', detail: 'Missing bearer token' });
    }
    try {
      const payload = jwt.verify(header.slice(7), secret, { algorithms: ALGORITHMS });
      if (!payload.sub) throw new Error('token has no subject');
      req.user = { id: payload.sub, email: payload.email };
      return next();
    } catch {
      return res.status(401).json({ title: 'Unauthorized', detail: 'Invalid or expired token' });
    }
  };
}
