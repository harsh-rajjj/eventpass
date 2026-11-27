-- Dev seed data: 5 organizers, 12 events, ~25 tiers
-- Dates use UTC. Some events in the past (for testing), most in the future.

INSERT INTO organizers (id, name, description, website, contact_email, status) VALUES
(1, 'Neon Festivals',      'Premium music & arts festivals across India',  'https://neonfest.example.com',  'contact@neonfest.example.com',  'APPROVED'),
(2, 'TechSphere',          'Developer conferences and hackathons',         'https://techsphere.example.com','hello@techsphere.example.com',  'APPROVED'),
(3, 'Urban Sports Co',     'City marathons, cycling and fitness events',   'https://urbansports.example.com','info@urbansports.example.com', 'APPROVED'),
(4, 'Laugh Factory India', 'Stand-up comedy tours',                        NULL,                            'bookings@laughfactory.example.com','APPROVED'),
(5, 'Starter Events',      'New organizer, not yet approved',              NULL,                            'admin@starter.example.com',     'PENDING');

INSERT INTO events (id, organizer_id, title, description, category, venue_name, city, address, starts_at, ends_at, status) VALUES
-- Neon Festivals (id=1)
(1,  1, 'Neon Sunburn 2027',        'Three-day electronic music festival featuring international DJs.', 'MUSIC',    'Vagator Beach Grounds',  'Goa',       '123 Beach Rd, Vagator',  '2027-02-14 14:00:00', '2027-02-16 23:00:00', 'PUBLISHED'),
(2,  1, 'Neon Acoustic Night',      'Intimate acoustic sessions under the stars.',                      'MUSIC',    'Cubbon Park Amphitheatre','Bangalore', NULL,                     '2027-03-20 18:00:00', '2027-03-20 22:00:00', 'PUBLISHED'),
(3,  1, 'Neon Holi Bash',           'Colors and beats — the biggest Holi party.',                       'MUSIC',    'JLN Stadium',            'Delhi',     'Lodhi Rd, New Delhi',    '2027-03-14 10:00:00', '2027-03-14 18:00:00', 'DRAFT'),

-- TechSphere (id=2)
(4,  2, 'DevConnect 2027',          'Full-stack developer conference with hands-on workshops.',         'TECH',     'HICC',                   'Hyderabad', 'Madhapur, Hyderabad',    '2027-04-05 09:00:00', '2027-04-06 18:00:00', 'PUBLISHED'),
(5,  2, 'AI Hack Day',              'Build an AI product in 24 hours.',                                 'WORKSHOP', 'WeWork Galaxy',          'Bangalore', '43 Residency Rd',        '2027-05-10 08:00:00', '2027-05-11 08:00:00', 'PUBLISHED'),
(6,  2, 'Cloud Summit (Cancelled)', 'Was planned but cancelled due to venue issues.',                   'TECH',     'Bombay Exhibition Ctr',  'Mumbai',    'Western Express Highway','2027-01-20 09:00:00', '2027-01-21 17:00:00', 'CANCELLED'),

-- Urban Sports Co (id=3)
(7,  3, 'Mumbai Marathon 2027',     'Full and half marathon through iconic Mumbai landmarks.',          'SPORTS',   'CST to Worli Sea Link',  'Mumbai',    'Chhatrapati Shivaji Terminus','2027-01-19 05:30:00','2027-01-19 13:00:00', 'PUBLISHED'),
(8,  3, 'Bangalore Cycling Tour',   '60km cycling tour through the garden city.',                      'SPORTS',   'Cubbon Park Start Line', 'Bangalore', 'Cubbon Park',            '2027-06-08 06:00:00', '2027-06-08 12:00:00', 'PUBLISHED'),

-- Laugh Factory India (id=4)
(9,  4, 'Zakir Khan Live',          'Haq se single — national tour stop.',                             'COMEDY',   'Phoenix Marketcity',     'Pune',      'Viman Nagar, Pune',      '2027-04-15 19:00:00', '2027-04-15 21:30:00', 'PUBLISHED'),
(10, 4, 'Comedy Open Mic',          'New comics get 7 minutes each.',                                  'COMEDY',   'The Habitat',            'Mumbai',    'Khar West',              '2027-05-01 20:00:00', '2027-05-01 22:00:00', 'PUBLISHED'),

-- Starter Events (id=5, PENDING organizer)
(11, 5, 'Starter Workshop',         'A draft event from a pending organizer.',                         'WORKSHOP', 'Some Venue',             'Delhi',     NULL,                     '2027-07-01 10:00:00', '2027-07-01 16:00:00', 'DRAFT'),

-- Past event (already happened)
(12, 1, 'Neon NYE 2026',            'New Year''s Eve celebration — already happened.',                  'MUSIC',    'Gateway Lawns',          'Mumbai',    'Apollo Bunder',          '2025-12-31 20:00:00', '2026-01-01 02:00:00', 'PUBLISHED');

INSERT INTO ticket_tiers (event_id, name, price, capacity, sold_count) VALUES
-- Neon Sunburn 2027 (event 1)
(1, 'General Admission', 2999.00,  5000, 3200),
(1, 'VIP',               7999.00,  500,  480),
(1, 'Backstage Pass',    14999.00, 50,   50),   -- sold out!

-- Neon Acoustic Night (event 2)
(2, 'Standard',          999.00,   200,  45),
(2, 'Premium (Front Row)', 2499.00, 30,  12),

-- DevConnect 2027 (event 4)
(4, 'Conference Pass',   4999.00,  1000, 310),
(4, 'Workshop Add-on',   1999.00,  200,  88),

-- AI Hack Day (event 5)
(5, 'Participant',       0.00,     150,  142),   -- free event, almost full

-- Mumbai Marathon (event 7)
(7, 'Full Marathon',     1500.00,  10000, 8900),
(7, 'Half Marathon',     800.00,   15000, 12300),
(7, '10K Fun Run',       400.00,   5000,  4100),

-- Bangalore Cycling Tour (event 8)
(8, 'Rider',             600.00,   500,  210),

-- Zakir Khan Live (event 9)
(9, 'Silver',            799.00,   800,  800),   -- sold out!
(9, 'Gold',              1499.00,  200,  195),
(9, 'Platinum',          2999.00,  50,   38),

-- Comedy Open Mic (event 10)
(10, 'Entry',            299.00,   100,  22),

-- Past event: Neon NYE 2026 (event 12)
(12, 'General',          3999.00,  3000, 3000),
(12, 'VVIP',             9999.00,  100,  100);
