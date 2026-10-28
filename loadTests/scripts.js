import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = 'http://host.docker.internal:8080';

// Custom counters so you can see the outcome mix, not just pass/fail
const created    = new Counter('payments_created');
const rejected   = new Counter('payments_rejected');
const idempotent = new Counter('payments_duplicate_hit');

export const options = {
    scenarios: {
        payments: {
            executor: 'ramping-vus',
            startVUs: 1,
            stages: [
                { duration: '30s', target: 10 },  // warm-up ramp
                { duration: '1m',  target: 30 },  // hold at 30
                { duration: '1m',  target: 50 },  // push to 50
                { duration: '30s', target: 0  },  // ramp down
            ],
            gracefulRampDown: '10s',
        },
    },
    thresholds: {
        http_req_failed:   ['rate<0.01'],           // <1% transport/5xx errors
        http_req_duration: ['p(95)<500', 'p(99)<1500'],
        'checks{type:created}': ['rate>0.95'],      // >95% of writes succeed
    },
};

// A stable prefix per test run, so reruns don't collide with old rows
const RUN_ID = `${Date.now()}-${Math.floor(Math.random() * 1e6)}`;

export default function () {
    // __VU and __ITER are unique per virtual user and per loop iteration.
    // RUN_ID isolates separate test executions from each other.
    const key   = `IDEM-${RUN_ID}-${__VU}-${__ITER}`;
    const userId = `Vishnu-${__VU}`;

    // Vary the amount slightly so each record is distinct too (optional but helps
    // if the service has any per-amount uniqueness or fraud rules).
    const amount = Number((1000 + (__ITER % 100) * 0.01).toFixed(2));

    const payload = JSON.stringify({
        clientId:      `C-${1000 + (__VU % 50)}`,   // spread across clients
        debitAccount:  'Jason M',
        creditAccount: 'Mehar Nolan',
        amount:        amount,
        currency:      'USD',
        reference:     key,                        // helpful for tracing in DB/logs
    });

    const params = {
        headers: {
            'Content-Type':    'application/json',
            'Idempotency-Key': key,
            'X-User-Id':       userId,
        },
        tags: { endpoint: 'POST /api/payments' },
    };

    const res = http.post(`${BASE}/api/payments`, payload, params);

    // Classify the outcome. Adjust the 200/201/202 split to match your controller.
    const ok = res.status >= 200 && res.status < 300;
    const isDup = res.status === 409 ||
        (res.body && /idempoten/i.test(res.body));

    if (ok)          created.add(1);
    else if (isDup)  idempotent.add(1);
    else             rejected.add(1);

    check(res, {
        'created (2xx)':        (r) => r.status >= 200 && r.status < 300,
        'not 5xx':              (r) => r.status < 500,
        'has body on success':  (r) => r.status >= 300 || (r.body && r.body.length > 0),
    }, { type: ok ? 'created' : 'other' });

    // Log a sample of failures so you can diagnose without drowning in output
    if (!ok && __ITER % 100 === 0) {
        console.warn(`status=${res.status} key=${key} body=${res.body?.slice(0, 200)}`);
    }
}