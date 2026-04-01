package com.maark.local;

import java.util.ArrayList;
import java.util.List;

public class SampleDataLoader {

    // ── helpers ────────────────────────────────────────────────────────────────

    private static void add(List<LocalDocument> list, String title, String content, String url) {
        list.add(new LocalDocument(title, content, url));
    }

    // ── topic data ─────────────────────────────────────────────────────────────

    // Each entry: { title, content snippet }
    private static final String[][] TECH = {
        {"Java Multithreading","Java multithreading enables concurrent execution via Thread and Runnable. ExecutorService manages thread pools efficiently."},
        {"Java Streams API","Java Streams provide a functional approach to processing collections with map, filter, reduce operations."},
        {"Java CompletableFuture","CompletableFuture enables async programming with thenApply, thenCompose, and allOf chaining."},
        {"Java Generics","Generics allow type-safe collections and methods. Wildcards and bounded types add flexibility."},
        {"Java Collections Framework","Java Collections include List, Set, Map, Queue. ArrayList, HashMap, and LinkedList are widely used."},
        {"Java Lambda Expressions","Lambdas enable concise functional-style code. They implement single-abstract-method interfaces."},
        {"Java Optional","Optional avoids NullPointerException by wrapping values. Use isPresent, orElse, and map."},
        {"Java Records","Records are immutable data classes introduced in Java 16 with auto-generated constructors and accessors."},
        {"Java Pattern Matching","Pattern matching with instanceof simplifies type checks and casts in Java 16+."},
        {"Java Virtual Threads","Virtual threads (Project Loom) provide lightweight concurrency with millions of threads simultaneously."},
        {"Python Basics","Python is interpreted, dynamically typed, and emphasizes readability with indentation-based syntax."},
        {"Python List Comprehensions","List comprehensions offer concise syntax: [expr for item in list if condition]."},
        {"Python Decorators","Decorators wrap functions to extend behavior using @syntax and functools.wraps."},
        {"Python AsyncIO","asyncio enables asynchronous I/O in Python using async/await syntax and event loops."},
        {"Python Data Classes","dataclasses reduce boilerplate by auto-generating __init__, __repr__, and __eq__."},
        {"Python Type Hints","Type hints improve code readability and enable static analysis via mypy and pyright."},
        {"Python Context Managers","Context managers using 'with' ensure resources like files are closed properly."},
        {"Python Generators","Generators use 'yield' to lazily produce values, saving memory for large datasets."},
        {"Python Virtual Environments","venv and virtualenv create isolated Python environments to avoid dependency conflicts."},
        {"Python Pip Package Manager","pip installs, upgrades, and manages Python packages from PyPI."},
        {"JavaScript ES6+","ES6 introduced arrow functions, template literals, destructuring, and modules."},
        {"JavaScript Promises","Promises handle async operations with then, catch, and finally chaining."},
        {"JavaScript Async Await","async/await simplifies promise-based code to synchronous-looking syntax."},
        {"JavaScript Closures","Closures allow inner functions to retain access to outer function variables."},
        {"JavaScript Prototype Chain","JavaScript uses prototype-based inheritance where objects inherit from other objects."},
        {"TypeScript Basics","TypeScript adds static types to JavaScript with interfaces, enums, and generics."},
        {"TypeScript Interfaces","Interfaces define object shapes. They support optional properties and readonly modifiers."},
        {"TypeScript Generics","TypeScript generics create reusable typed components and functions."},
        {"Node.js Introduction","Node.js is a JavaScript runtime built on Chrome V8 for server-side programming."},
        {"Node.js Event Loop","The event loop handles async I/O non-blocking operations in a single thread."},
        {"React Introduction","React is a UI library using components, JSX, and a virtual DOM for efficient rendering."},
        {"React Hooks","useState, useEffect, useContext, useMemo, and useCallback are core React hooks."},
        {"React Context API","Context API provides global state management without prop drilling."},
        {"React Performance","Use React.memo, useMemo, useCallback, and lazy loading to optimize performance."},
        {"React Router","React Router enables client-side navigation with Route, Link, and useNavigate."},
        {"Vue.js Basics","Vue.js is a progressive framework with reactive data binding and component system."},
        {"Angular Introduction","Angular is a TypeScript framework with dependency injection and two-way data binding."},
        {"Next.js Framework","Next.js provides server-side rendering, static generation, and file-based routing for React."},
        {"GraphQL Basics","GraphQL is a query language for APIs enabling precise data fetching with a single endpoint."},
        {"REST API Design","REST uses HTTP methods GET, POST, PUT, DELETE with stateless communication and JSON."},
        {"Docker Basics","Docker packages apps in containers ensuring consistent environments across machines."},
        {"Docker Compose","Docker Compose defines multi-container apps in YAML with a single command to start all."},
        {"Kubernetes Introduction","Kubernetes orchestrates containers with pods, services, deployments, and auto-scaling."},
        {"Kubernetes Helm","Helm is the package manager for Kubernetes using charts to deploy applications."},
        {"CI/CD Pipelines","CI/CD automates build, test, deploy cycles using Jenkins, GitHub Actions, or GitLab CI."},
        {"Git Version Control","Git tracks source code changes with commit, branch, merge, rebase, and pull requests."},
        {"GitHub Actions","GitHub Actions automates workflows directly in repositories with YAML-based pipelines."},
        {"Linux Command Line","Essential Linux commands: ls, cd, grep, find, chmod, sudo, top, curl, wget, ssh."},
        {"Bash Scripting","Bash scripts automate tasks using variables, loops, conditionals, and functions."},
        {"SQL Basics","SQL queries data with SELECT, INSERT, UPDATE, DELETE, JOIN, GROUP BY, and ORDER BY."},
        {"SQL Joins","INNER, LEFT, RIGHT, FULL OUTER joins combine rows from multiple tables by key relationships."},
        {"PostgreSQL Introduction","PostgreSQL is an open-source RDBMS with ACID compliance, JSON support, and extensions."},
        {"MySQL Basics","MySQL is a widely used relational database with fast reads and strong community support."},
        {"MongoDB Introduction","MongoDB stores documents in JSON-like BSON format enabling flexible schema design."},
        {"Redis Cache","Redis is an in-memory key-value store used for caching, sessions, and pub/sub messaging."},
        {"Elasticsearch Basics","Elasticsearch is a distributed search engine built on Lucene for full-text and analytics."},
        {"Apache Kafka","Kafka is a distributed event streaming platform for high-throughput real-time pipelines."},
        {"RabbitMQ Messaging","RabbitMQ implements AMQP for reliable message queuing between microservices."},
        {"Microservices Architecture","Microservices decompose apps into small independent services communicating over APIs."},
        {"Spring Boot Basics","Spring Boot simplifies Java app creation with auto-configuration and embedded servers."},
        {"Spring Security","Spring Security handles authentication, authorization, and CSRF protection in Java apps."},
        {"Hibernate ORM","Hibernate maps Java objects to database tables, handling CRUD with JPA annotations."},
        {"Maven Build Tool","Maven manages Java project dependencies and builds using pom.xml configuration."},
        {"Gradle Build Tool","Gradle uses Groovy/Kotlin DSL for flexible, incremental, and fast build automation."},
        {"Design Patterns Singleton","Singleton ensures one instance per class using private constructor and static getInstance."},
        {"Design Patterns Factory","Factory pattern creates objects without specifying exact classes, promoting loose coupling."},
        {"Design Patterns Observer","Observer defines one-to-many dependency; subjects notify observers on state changes."},
        {"Design Patterns Strategy","Strategy pattern defines a family of algorithms and makes them interchangeable."},
        {"SOLID Principles","SOLID: Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion."},
        {"Clean Code Practices","Clean code is readable, maintainable, uses meaningful names, short functions, and avoids duplication."},
        {"Data Structures Arrays","Arrays are fixed-size indexed collections with O(1) access and O(n) insertion."},
        {"Data Structures Linked Lists","Linked lists have O(1) insert at head, O(n) search, made of nodes with next pointers."},
        {"Data Structures Hash Maps","Hash maps offer O(1) average lookup using hash functions to map keys to buckets."},
        {"Data Structures Trees","Binary trees, BSTs, AVL, and Red-Black trees for hierarchical data and efficient search."},
        {"Data Structures Graphs","Graphs model networks using adjacency lists/matrices; traversed via BFS and DFS."},
        {"Big O Notation","O(1), O(log n), O(n), O(n log n), O(n²) classify algorithm time and space complexity."},
        {"Sorting Algorithms","QuickSort, MergeSort, HeapSort, BubbleSort — tradeoffs in time complexity and stability."},
        {"Binary Search","Binary search finds elements in sorted arrays in O(log n) by halving the search space."},
        {"Dynamic Programming","DP solves complex problems by breaking into overlapping subproblems and memoizing results."},
        {"Machine Learning Basics","ML uses data to train models for prediction using supervised/unsupervised/reinforcement learning."},
        {"Neural Networks","Neural networks use layers of neurons with weights and activation functions to learn patterns."},
        {"Deep Learning","Deep learning uses many-layered networks for images, speech, and natural language tasks."},
        {"Natural Language Processing","NLP enables computers to understand text via tokenization, parsing, and transformers."},
        {"Large Language Models","LLMs like GPT and Gemini use transformer architecture trained on massive text datasets."},
        {"Computer Vision","CV algorithms detect and classify objects in images using CNNs and YOLO architectures."},
        {"TensorFlow Basics","TensorFlow is an open-source ML framework for building and training neural networks."},
        {"PyTorch Basics","PyTorch provides dynamic computation graphs and eager execution for deep learning research."},
        {"scikit-learn","scikit-learn offers ML algorithms for classification, regression, clustering, and pipelines."},
        {"Pandas Library","Pandas provides DataFrames for data manipulation, analysis, and CSV/JSON handling in Python."},
        {"NumPy Basics","NumPy provides N-dimensional arrays and mathematical operations for scientific computing."},
        {"Matplotlib Visualization","Matplotlib creates plots, histograms, scatter charts, and subplots for data visualization."},
        {"Jupyter Notebooks","Jupyter combines code, output, and markdown in interactive notebooks for data science."},
        {"Cloud Computing AWS","AWS offers EC2, S3, Lambda, RDS, and hundreds of cloud services on demand."},
        {"Google Cloud Platform","GCP provides BigQuery, Cloud Run, GKE, Cloud Storage, and AI/ML services."},
        {"Microsoft Azure","Azure offers virtual machines, AKS, Azure Functions, and Cognitive Services."},
        {"Serverless Computing","Serverless runs code on demand without managing servers using AWS Lambda or Cloud Functions."},
        {"Infrastructure as Code","Terraform, Pulumi, and CloudFormation define cloud infrastructure in declarative files."},
        {"API Gateway","API gateways handle routing, authentication, rate limiting, and monitoring for microservices."},
        {"OAuth2 Authentication","OAuth2 provides authorization flows: authorization code, client credentials, and PKCE."},
        {"JWT Tokens","JWTs encode claims as signed JSON objects for stateless authentication between services."},
        {"WebSockets","WebSockets provide full-duplex communication over a single TCP connection for real-time apps."},
        {"gRPC Protocol","gRPC uses Protocol Buffers and HTTP/2 for high-performance RPC between services."},
        {"Apache Lucene","Lucene is a Java full-text search library powering Elasticsearch and Solr engines."},
        {"Regex Patterns","Regular expressions match text patterns: \\d digits, \\w word chars, .* any sequence."},
        {"Unit Testing JUnit","JUnit 5 tests Java code with @Test, @BeforeEach, assertions, and parameterized tests."},
        {"Mockito Testing","Mockito mocks dependencies in Java unit tests using when, verify, and spy."},
        {"Test-Driven Development","TDD writes failing tests first, implements code to pass, then refactors."},
        {"Code Review Best Practices","Good reviews check logic, readability, test coverage, security, and performance."},
        {"Agile Methodology","Agile delivers software in iterations with sprints, retrospectives, and daily standups."},
        {"Scrum Framework","Scrum uses sprints, product backlog, sprint planning, reviews, and retrospectives."},
        {"System Design Basics","System design covers scalability, load balancing, caching, databases, and fault tolerance."},
        {"Load Balancing","Load balancers distribute traffic across servers using round-robin, least-connections, or IP hash."},
        {"Caching Strategies","Cache-aside, write-through, and write-behind are strategies for keeping data in sync."},
        {"Rate Limiting","Rate limiting protects APIs using token bucket, leaky bucket, and sliding window algorithms."},
        {"Content Delivery Network","CDNs cache content at edge locations reducing latency for global users."},
        {"Cybersecurity Basics","Cybersecurity protects systems via firewalls, encryption, authentication, and patch management."},
        {"SQL Injection","SQL injection attacks insert malicious SQL; prevent with parameterized queries and ORM."},
        {"Cross-Site Scripting","XSS injects scripts into web pages; prevent with Content Security Policy and escaping."},
        {"HTTPS TLS","TLS encrypts data in transit; HTTPS uses certificates, handshakes, and symmetric encryption."},
        {"Blockchain Basics","Blockchain stores data in chained blocks verified by consensus mechanisms like PoW and PoS."},
        {"Web3 Development","Web3 uses smart contracts, wallets, and decentralized applications on blockchain networks."},
        {"WebAssembly","WASM runs compiled C/C++/Rust code in browsers at near-native speed."},
        {"Progressive Web Apps","PWAs use service workers, manifests, and caching for offline-capable web applications."},
    };

    private static final String[][] HEALTH = {
        {"How to lose weight fast","Losing weight requires caloric deficit, regular exercise, and balanced nutrition."},
        {"Mediterranean diet benefits","Mediterranean diet reduces heart disease risk with olive oil, fish, and vegetables."},
        {"Intermittent fasting guide","Intermittent fasting cycles eating and fasting windows like 16:8 or 5:2 methods."},
        {"How to sleep better","Good sleep needs consistent schedule, dark room, no screens, and 7-9 hours nightly."},
        {"Benefits of exercise","Regular exercise improves heart health, mood, weight, and reduces chronic disease risk."},
        {"Symptoms of diabetes","Diabetes symptoms: frequent urination, thirst, fatigue, blurred vision, slow wound healing."},
        {"How to reduce anxiety","Reduce anxiety with breathing exercises, therapy, exercise, limiting caffeine, and sleep."},
        {"Blood pressure normal range","Normal blood pressure is 120/80 mmHg; above 130/80 is considered hypertension."},
        {"Vitamin D deficiency signs","Vitamin D deficiency causes fatigue, bone pain, muscle weakness, and depression."},
        {"How to boost immunity","Boost immunity with sleep, exercise, balanced diet, stress management, and vitamins C/D."},
        {"COVID-19 symptoms","COVID symptoms: fever, cough, fatigue, loss of smell/taste, shortness of breath."},
        {"Mental health awareness","Mental health includes emotional, psychological well-being. Therapy and support improve outcomes."},
        {"Depression signs and help","Depression causes persistent sadness, loss of interest, sleep changes, and low energy."},
        {"Yoga benefits for health","Yoga improves flexibility, strength, balance, stress, and mental clarity with regular practice."},
        {"Meditation techniques","Mindfulness, body scan, loving-kindness, and breath-focus meditations reduce stress."},
        {"Keto diet explained","Keto diet uses high fat, low carb intake to enter ketosis for fat burning."},
        {"Hydration importance","Adults need 8 cups of water daily. Dehydration causes headaches, fatigue, poor concentration."},
        {"Cholesterol levels guide","LDL under 100, HDL above 60, total under 200 mg/dL for optimal heart health."},
        {"How to quit smoking","Quit smoking using nicotine replacement, medication, counseling, and behavioral strategies."},
        {"Back pain remedies","Back pain relief: ice/heat, stretching, posture correction, core strengthening, and rest."},
        {"Calories in common foods","Chicken breast 165 cal, apple 95 cal, egg 78 cal, rice cup 206 cal per serving."},
        {"Protein intake daily","Adults need 0.8g protein per kg bodyweight; athletes may need 1.2-2.0g per kg."},
        {"Iron deficiency anemia","Iron deficiency causes fatigue, pale skin, shortness of breath, and weakness."},
        {"Thyroid symptoms","Thyroid issues cause weight changes, fatigue, mood swings, and temperature sensitivity."},
        {"How to manage stress","Manage stress with exercise, journaling, social support, mindfulness, and time management."},
        {"Pregnancy first trimester","First trimester includes morning sickness, fatigue, weight gain, and fetal organ development."},
        {"Baby development milestones","Babies smile at 2 months, sit at 6 months, walk at 12 months, talk at 18 months."},
        {"ADHD symptoms in adults","Adult ADHD: inattention, hyperactivity, impulsivity, poor time management, forgetfulness."},
        {"Autism spectrum signs","Autism signs: difficulty with social interaction, repetitive behaviors, sensory sensitivities."},
        {"Alzheimer's early signs","Early Alzheimer's: memory loss, confusion with time/place, difficulty with familiar tasks."},
        {"Cancer prevention tips","Reduce cancer risk with no smoking, healthy diet, exercise, sun protection, and screenings."},
        {"Heart disease prevention","Prevent heart disease with diet, exercise, no smoking, blood pressure, and cholesterol control."},
        {"Allergies vs cold symptoms","Allergies: itchy eyes/nose, sneezing, no fever. Cold: sore throat, fever, body aches."},
        {"Food allergy list","Common food allergies: peanuts, tree nuts, milk, eggs, wheat, soy, fish, shellfish."},
        {"Migraine triggers","Migraine triggers: stress, hormones, sleep changes, bright lights, strong smells, caffeine."},
        {"How to stop headaches","Headache relief: stay hydrated, rest, pain relievers, cold/hot compress, reduce stress."},
    };

    private static final String[][] FINANCE = {
        {"How to invest money","Start investing with emergency fund, then index funds, ETFs, and retirement accounts."},
        {"Stock market basics","Stocks represent company ownership. Markets fluctuate based on earnings, news, and economy."},
        {"Bitcoin price today","Bitcoin price fluctuates 24/7 based on demand, regulations, and market sentiment."},
        {"How to save money","Save money with budgeting, 50/30/20 rule, automatic transfers, and cutting subscriptions."},
        {"Credit score improvement","Improve credit: pay bills on time, reduce utilization, avoid new hard inquiries."},
        {"Best credit cards 2024","Top credit cards offer cashback, travel rewards, no annual fee, or low APR."},
        {"Home buying guide","Buying a home needs pre-approval, down payment, inspection, and understanding mortgage terms."},
        {"Mortgage rates today","Mortgage rates vary by credit score, loan type, and Federal Reserve interest rate decisions."},
        {"ETF vs mutual funds","ETFs trade like stocks intraday; mutual funds price once daily. ETFs have lower fees."},
        {"401k vs IRA","401k is employer plan; IRA is individual. Both offer tax advantages for retirement saving."},
        {"Tax deductions list","Deductions include mortgage interest, charitable donations, medical expenses, business costs."},
        {"How to file taxes","File taxes using W-2 forms, deductions, and tax software like TurboTax or FreeTaxUSA."},
        {"Personal budgeting tips","Track income and expenses, categorize spending, set goals, and review monthly."},
        {"Emergency fund size","Emergency fund should cover 3-6 months of living expenses in a high-yield savings account."},
        {"Inflation explained","Inflation reduces purchasing power over time; Federal Reserve raises rates to control it."},
        {"Cryptocurrency investing","Crypto investing is high-risk; diversify, research projects, and only invest what you can lose."},
        {"Dividend investing guide","Dividend stocks pay regular income; focus on yield, payout ratio, and growth consistency."},
        {"Index fund investing","Index funds track market indices like S&P 500 with low fees and broad diversification."},
        {"Compound interest power","Compound interest grows money exponentially; start early for maximum long-term benefit."},
        {"Debt payoff strategies","Avalanche pays highest-interest debt first; snowball pays smallest balance first for motivation."},
        {"Dollar cost averaging","DCA invests fixed amounts regularly, reducing impact of market volatility over time."},
        {"Roth IRA benefits","Roth IRA grows tax-free; contributions (after-tax) can be withdrawn anytime penalty-free."},
        {"Real estate investment","Real estate generates rental income and appreciation; REITs offer stock-market exposure."},
        {"Side hustle ideas","Side hustles: freelancing, tutoring, rideshare, dropshipping, content creation, reselling."},
        {"Passive income streams","Passive income: dividends, rental income, affiliate marketing, digital products, royalties."},
    };

    private static final String[][] LIFESTYLE = {
        {"How to wake up early","Wake early by adjusting bedtime gradually, avoiding screens, and creating morning routine."},
        {"Productivity tips","Use time blocking, Pomodoro, task lists, deep work sessions, and minimize distractions."},
        {"Work from home tips","WFH success needs dedicated workspace, schedule, breaks, video calls, and clear boundaries."},
        {"How to learn faster","Learn faster with spaced repetition, active recall, teaching others, and focused practice."},
        {"Morning routine ideas","Morning routines with exercise, journaling, meditation, healthy breakfast boost productivity."},
        {"How to read more books","Read more by carrying a book, setting daily pages goals, and reducing screen time."},
        {"Travel on a budget","Budget travel with flight deals, hostels, cooking meals, free attractions, and off-peak timing."},
        {"Best travel destinations 2024","Top destinations: Japan, Portugal, New Zealand, Peru, Morocco, Iceland for 2024."},
        {"Minimalism lifestyle","Minimalism reduces clutter, stress, and spending by owning only what adds value."},
        {"How to declutter home","Declutter using KonMari method: keep only items that spark joy and categorize by type."},
        {"Healthy meal prep","Meal prep saves time and money; batch cook proteins, grains, and vegetables weekly."},
        {"Easy recipes for beginners","Beginners start with scrambled eggs, pasta, stir-fry, soups, and sheet-pan meals."},
        {"Home workout routine","No-gym workouts: push-ups, squats, lunges, planks, and burpees for full body fitness."},
        {"Best podcasts to listen to","Top podcasts: Joe Rogan, How I Built This, Serial, Radiolab, Lex Fridman, TED Talks."},
        {"Self improvement books","Best self-help books: Atomic Habits, Deep Work, Thinking Fast and Slow, Mindset."},
        {"How to make friends","Make friends by joining clubs, being curious, listening actively, and following up consistently."},
        {"Time management strategies","Prioritize with Eisenhower Matrix, say no to low-priority tasks, batch similar work."},
        {"How to be more confident","Build confidence with preparation, positive self-talk, body language, and small wins."},
        {"Journaling benefits","Journaling reduces anxiety, improves clarity, tracks goals, and builds self-awareness."},
        {"Social media detox","Social media detox improves focus, sleep, and well-being by reducing compulsive checking."},
        {"How to stop procrastinating","Beat procrastination with two-minute rule, breaking tasks small, and removing distractions."},
        {"Goal setting techniques","SMART goals are Specific, Measurable, Achievable, Relevant, Time-bound for success."},
        {"Habits and behavior change","James Clear's Atomic Habits: make good habits obvious, attractive, easy, and satisfying."},
        {"How to network professionally","Network by adding value, following up, attending events, and connecting on LinkedIn."},
        {"Learning a new language","Language learning: Duolingo, Anki cards, immersion, conversation practice, daily consistency."},
    };

    private static final String[][] EDUCATION = {
        {"How to study effectively","Study effectively using spaced repetition, active recall, pomodoro, and practice tests."},
        {"Best online learning platforms","Coursera, edX, Udemy, Khan Academy, and LinkedIn Learning offer quality courses."},
        {"GRE preparation tips","GRE prep: practice tests, vocabulary, quant drills, official ETS materials, 3-month plan."},
        {"IELTS exam guide","IELTS tests listening, reading, writing, speaking. Target band 7+ for university admission."},
        {"How to write an essay","Essay writing: strong thesis, structured paragraphs, evidence, transitions, and proofreading."},
        {"Scholarship search tips","Find scholarships on Fastweb, Scholarship.com, your college, and local organizations."},
        {"Computer science degree","CS degrees cover algorithms, data structures, OS, networks, databases, and software engineering."},
        {"MBA programs guide","MBA programs offer finance, marketing, strategy, leadership, and networking opportunities."},
        {"Data science career path","Data science needs Python, SQL, statistics, ML, and domain knowledge for career success."},
        {"Software engineering skills","SE skills: coding, system design, testing, version control, collaboration, and problem-solving."},
        {"UX design basics","UX design focuses on user research, wireframing, prototyping, usability testing, and iteration."},
        {"Digital marketing skills","Digital marketing: SEO, SEM, social media, email, content, and analytics for growth."},
        {"Graphic design tools","Figma, Adobe Illustrator, Photoshop, and Canva are essential graphic design tools."},
        {"Public speaking tips","Public speaking: know your topic, practice, eye contact, pause for emphasis, and breathe."},
        {"Resume writing guide","Resume: one page, strong action verbs, quantified achievements, ATS keywords, and clean format."},
        {"Job interview preparation","Interview prep: research company, STAR answers, prepare questions, dress appropriately."},
        {"LinkedIn profile optimization","LinkedIn: professional photo, keyword-rich headline, detailed experience, and recommendations."},
        {"How to negotiate salary","Negotiate salary with market research, anchoring high, emphasizing value, and practicing."},
        {"Remote job search","Find remote jobs on Remote.co, We Work Remotely, LinkedIn, Upwork, and Toptal."},
        {"Freelancing guide","Freelancing needs portfolio, client communication, pricing, contracts, and consistent outreach."},
    };

    private static final String[][] ENTERTAINMENT = {
        {"Best movies 2024","Top 2024 movies: Dune Part 2, Inside Out 2, Deadpool vs Wolverine, Alien Romulus."},
        {"Netflix shows to watch","Netflix hits: Stranger Things, The Crown, Wednesday, Squid Game, Ozark, Narcos."},
        {"Best TV series ever","Best shows: Breaking Bad, The Wire, The Sopranos, Game of Thrones, Fleabag."},
        {"Spotify top songs","Spotify charts feature pop, hip-hop, and Latin hits updated weekly globally."},
        {"Best video games 2024","Top 2024 games: Elden Ring DLC, Helldivers 2, Final Fantasy VII Rebirth, Balatro."},
        {"How to start a YouTube channel","YouTube growth: niche focus, consistent uploads, SEO thumbnails, and audience engagement."},
        {"TikTok viral trends","TikTok trends spread quickly through sounds, challenges, and duet features globally."},
        {"Instagram growth tips","Instagram: consistent posting, Reels, hashtags, stories, and authentic engagement grow accounts."},
        {"Sports news today","Sports news covers NFL, NBA, Premier League, MLB, and international tournaments daily."},
        {"FIFA World Cup schedule","World Cup held every 4 years with 32 teams competing through group and knockout stages."},
        {"Olympics 2024 highlights","Paris 2024 Olympics featured athletics, swimming, gymnastics, and over 10,000 athletes."},
        {"Best anime series","Top anime: Attack on Titan, Death Note, Fullmetal Alchemist: Brotherhood, One Piece, Naruto."},
        {"Manga to read","Popular manga: One Piece, Jujutsu Kaisen, Berserk, Chainsaw Man, Demon Slayer."},
        {"Harry Potter series","Harry Potter is a 7-book fantasy series by J.K. Rowling about a young wizard."},
        {"Marvel movies order","Watch Marvel movies in release or chronological order starting with Captain America: First Avenger."},
        {"Board games for adults","Popular adult board games: Catan, Ticket to Ride, Codenames, Azul, Wingspan."},
        {"Best books to read 2024","2024 reads: James, Intermezzo, All Fours, The Women, James by Percival Everett."},
        {"Celebrity news today","Celebrity gossip covers relationships, awards, feuds, and social media dramas daily."},
        {"Music festival schedule","Major festivals: Coachella, Glastonbury, Lollapalooza, Tomorrowland, Reading Festival."},
        {"Cooking shows Netflix","Popular cooking shows: Chef's Table, Salt Fat Acid Heat, Street Food, The Bear."},
    };

    private static final String[][] SHOPPING = {
        {"Amazon deals today","Amazon features daily deals, lightning deals, and seasonal sales on millions of items."},
        {"iPhone latest model","Apple iPhone releases annually with improved cameras, chips, and display technology."},
        {"Best laptops 2024","Top laptops 2024: MacBook Pro M3, Dell XPS, Lenovo ThinkPad X1, ASUS ZenBook."},
        {"Best headphones under 100","Budget headphones: Sony WH-CH720N, Anker Q20, JBL Tune 670NC offer great value."},
        {"Smart home devices","Smart home: Google Nest, Amazon Echo, Philips Hue, Ring Doorbell, and smart plugs."},
        {"Gaming chair reviews","Gaming chairs: Secretlab, DXRacer, Corsair, and Herman Miller for ergonomic comfort."},
        {"Standing desk benefits","Standing desks reduce back pain, improve posture, and increase energy during work hours."},
        {"Best running shoes","Top running shoes 2024: ASICS Gel-Kayano, Brooks Ghost, Nike Pegasus, New Balance 1080."},
        {"Skincare routine basics","Basic skincare: cleanser, toner, moisturizer, SPF. Add serums for targeted concerns."},
        {"Best electric cars 2024","Top EVs 2024: Tesla Model 3, Rivian R1T, Ford Mustang Mach-E, Hyundai Ioniq 6."},
        {"Airpods vs Sony WH1000XM5","AirPods lead in Apple ecosystem; Sony WH-1000XM5 wins for noise cancellation quality."},
        {"Best coffee makers","Top coffee makers: Breville Barista Express, Nespresso Vertuo, Chemex, AeroPress."},
        {"Budget travel bags","Budget travel bags: Osprey Farpoint, AmazonBasics, Tortuga, and Nomatic for trips."},
        {"Online shopping safety","Shop safely: use HTTPS sites, avoid public WiFi for payments, use credit not debit."},
        {"Black Friday deals","Black Friday offers major discounts on electronics, clothing, and appliances from major retailers."},
        {"Grocery delivery services","Grocery delivery: Instacart, Amazon Fresh, Walmart Grocery, and DoorDash Dash Mart."},
        {"Home office setup","Home office essentials: ergonomic chair, monitor, webcam, desk lamp, and fast internet."},
        {"Best mattress for back pain","Best mattresses: Saatva Classic, Purple Hybrid, Tempur-Pedic, and Casper Wave."},
        {"Fitness tracker comparison","Fitness trackers: Fitbit Charge 6, Garmin Vivosmart 5, Apple Watch SE for health."},
        {"Smart TV buying guide","Smart TVs: Samsung QLED, LG OLED, Sony Bravia, and TCL Roku for picture quality."},
    };

    private static final String[][] FOOD = {
        {"How to make pasta","Boil salted water, cook pasta al dente, combine with sauce made from garlic and tomatoes."},
        {"Best pizza recipes","Classic pizza needs 00 flour dough, San Marzano tomatoes, fresh mozzarella, and basil."},
        {"Chicken recipes easy","Easy chicken: grilled lemon herb, butter chicken, stir-fry, sheet-pan roasted, or soup."},
        {"Smoothie recipes healthy","Healthy smoothies: banana spinach, berry almond, mango ginger, and green detox blends."},
        {"How to bake bread","Bread baking needs flour, yeast, water, salt, kneading, proofing, and oven baking."},
        {"Vegan recipes for beginners","Vegan beginners start with Buddha bowls, lentil soups, chickpea curry, and tofu stir-fry."},
        {"Sushi making at home","Home sushi needs sushi rice, nori sheets, fresh fish or vegetables, and bamboo mat."},
        {"Weight loss meal plan","Weight loss meals: lean protein, vegetables, whole grains, and minimal processed food."},
        {"Coffee types explained","Coffee types: espresso, latte, cappuccino, americano, flat white, cold brew, and macchiato."},
        {"Wine pairing guide","Wine pairing: red with meat, white with fish, rosé with lighter dishes, sparkling for celebration."},
        {"Air fryer recipes","Air fryer cooks fries, chicken wings, vegetables, and even cakes with less oil quickly."},
        {"Instant Pot recipes","Instant Pot makes soups, stews, rice, chicken, and beans in fraction of normal time."},
        {"Gluten-free baking","Gluten-free baking uses almond, rice, or oat flour with xanthan gum for structure."},
        {"Meal prep ideas","Prep grains, proteins, and vegetables weekly; combine into varied meals throughout the week."},
        {"Best restaurants near me","Search Google Maps or Yelp for top-rated restaurants with photos and reviews nearby."},
        {"Cocktail recipes","Classic cocktails: Margarita, Old Fashioned, Negroni, Mojito, Cosmopolitan, Daiquiri."},
        {"How to make sourdough","Sourdough needs starter culture, bulk fermentation, shaping, cold proofing, and Dutch oven baking."},
        {"Protein foods list","High protein foods: chicken, eggs, Greek yogurt, cottage cheese, lentils, tofu, quinoa."},
        {"Vegetarian recipes","Vegetarian meals: veggie stir-fry, lentil dal, mushroom risotto, caprese salad, stuffed peppers."},
        {"Dessert recipes easy","Easy desserts: chocolate lava cake, no-bake cheesecake, cookies, brownies, panna cotta."},
    };

    private static final String[][] TRAVEL = {
        {"Best places to visit in Europe","Europe highlights: Paris, Rome, Barcelona, Amsterdam, Prague, Santorini, Dubrovnik."},
        {"Japan travel guide","Japan: visit Tokyo, Kyoto, Osaka, Hiroshima; experience temples, sushi, and cherry blossoms."},
        {"Visa requirements guide","Visa needs vary by passport and destination; check embassy websites or VisaHQ."},
        {"Best beaches in the world","Top beaches: Bora Bora, Maldives, Whitehaven Australia, Navagio Greece, Seven Mile Cayman."},
        {"Budget backpacking Southeast Asia","Southeast Asia budget travel: Thailand, Vietnam, Cambodia, Bali on under $50 a day."},
        {"USA national parks guide","Top national parks: Yellowstone, Yosemite, Grand Canyon, Zion, Great Smoky Mountains."},
        {"How to find cheap flights","Cheap flights: use Google Flights, Skyscanner, be flexible on dates, book 6-8 weeks out."},
        {"Travel insurance importance","Travel insurance covers cancellations, medical emergencies, lost luggage, and delays."},
        {"Packing list essentials","Packing essentials: passport, charger, medications, versatile clothes, toiletries, and cash."},
        {"Airbnb vs hotels","Airbnb suits longer stays and groups; hotels offer services, consistency, and points programs."},
        {"Road trip planning","Plan road trips with route apps, accommodations, emergency kit, and flexible daily mileage."},
        {"Cruise travel guide","Cruises include meals, entertainment, and ports of call; book early for best prices."},
        {"Solo travel safety tips","Solo travel: share itinerary, stay aware, trust instincts, use reputable transport, and insure."},
        {"Travel hacks for saving money","Travel hacks: use points, travel off-peak, eat local, walk instead of taxis, carry snacks."},
        {"Best travel apps","Travel apps: Google Maps, TripAdvisor, Booking.com, Duolingo, XE Currency, PackPoint."},
        {"Honeymoon destinations","Honeymoon spots: Maldives, Bali, Amalfi Coast, Santorini, Fiji, Hawaii, Paris."},
        {"Family vacation ideas","Family trips: Disney World, beach resorts, national parks, cruise ships, and theme parks."},
        {"Work and travel visa","Working holiday visas available in Australia, New Zealand, Canada, Germany, Ireland."},
        {"Digital nomad destinations","Nomad hotspots: Lisbon, Bali, Chiang Mai, Tbilisi, Mexico City, Medellín for remote work."},
        {"Airport tips for travel","Airport tips: check-in online, arrive early, TSA PreCheck, eat before security, charge devices."},
    };

    private static final String[][] NEWS_GENERAL = {
        {"Latest world news","World news covers politics, conflicts, economy, climate, and diplomatic events globally."},
        {"Election results 2024","2024 elections across USA, India, UK, EU feature major political shifts and voter turnout."},
        {"Climate change news","Climate change news covers temperature records, emissions, policies, and extreme weather events."},
        {"Artificial intelligence news","AI news: new models, regulations, job impact, generative AI tools, and ethics debates."},
        {"Space exploration news","Space news: NASA Artemis, SpaceX Starship, Mars missions, and private space tourism updates."},
        {"Economic news today","Economic news: inflation, job reports, Fed rates, GDP growth, and market performance."},
        {"Sports scores today","Sports scores: NFL Sunday results, NBA standings, Premier League goals, and MLB highlights."},
        {"Tech company news","Tech news: Apple, Google, Microsoft, Amazon, Meta earnings, products, and legal updates."},
        {"Science discoveries 2024","Science news covers new species, physics breakthroughs, genetics, and medical advances."},
        {"Environmental news","Environmental news: deforestation, ocean pollution, renewable energy growth, and conservation."},
        {"COVID pandemic updates","Post-COVID news covers new variants, long COVID research, and healthcare recovery."},
        {"Political news USA","US politics: Congress debates, presidential policies, Supreme Court decisions, and legislation."},
        {"Ukraine war news","Ukraine conflict news covers battlefield updates, international aid, and peace negotiations."},
        {"Middle East conflict news","Middle East news covers Gaza, Israel-Palestine conflict, humanitarian situation, and diplomacy."},
        {"Earthquake news today","Earthquake news reports magnitude, location, casualties, and aftershock activity globally."},
        {"Wildfire news","Wildfire coverage includes location, acreage, evacuation orders, and containment efforts."},
        {"Hurricane weather news","Hurricane news covers track, wind speed, storm surge, and evacuation warnings."},
        {"Cryptocurrency news today","Crypto news: Bitcoin ETF, regulatory updates, exchange news, and market movements."},
        {"Health news breakthrough","Health news covers new treatments, clinical trials, drug approvals, and disease research."},
        {"Celebrity news gossip","Celebrity news: relationships, awards, controversies, social media posts, and entertainment."},
    };

    private static final String[][] HOWTO = {
        {"How to tie a tie","Windsor knot steps: drape tie, cross wide end over narrow, loop, tighten, adjust."},
        {"How to change a tire","Tire change: loosen lug nuts, jack up car, swap tire, tighten lug nuts in star pattern."},
        {"How to write a cover letter","Cover letter: address hiring manager, match job keywords, show enthusiasm, and be concise."},
        {"How to do CPR","CPR: 30 chest compressions, 2 rescue breaths, repeat at 100-120 compressions per minute."},
        {"How to unclog a drain","Unclog drains with boiling water, baking soda and vinegar, or a plunger and drain snake."},
        {"How to change oil","Oil change: drain old oil, replace filter, add new oil, check level, dispose properly."},
        {"How to sew basics","Sewing basics: thread needle, use backstitch for strength, pin fabric, and press seams."},
        {"How to paint a room","Paint a room: tape edges, prime walls, cut in corners, roll main area, let dry."},
        {"How to fix leaky faucet","Fix faucet: turn off water, remove handle, replace washer or cartridge, reassemble."},
        {"How to make a budget","Budgeting: list income, track expenses, categorize, set limits, and review monthly."},
        {"How to start a business","Start business: validate idea, write plan, register legally, build product, and market."},
        {"How to do taxes yourself","DIY taxes: gather W-2s, use TurboTax or IRS Free File, claim deductions, e-file."},
        {"How to invest in stocks","Stock investing: open brokerage, research companies, diversify, and invest long-term."},
        {"How to make money online","Make money online: freelancing, affiliate marketing, selling courses, dropshipping, content."},
        {"How to lose belly fat","Lose belly fat with caloric deficit, cardio, strength training, sleep, and reduced sugar."},
        {"How to grow plants","Plant growth needs right soil, sunlight, watering schedule, fertilizer, and pruning."},
        {"How to fix credit score","Fix credit: pay on time, reduce credit utilization, dispute errors, and be patient."},
        {"How to write a business plan","Business plan needs executive summary, market analysis, financials, and marketing strategy."},
        {"How to speak another language","Language learning: daily practice, immersion, speaking with natives, and consistent study."},
        {"How to get rid of pests","Pest control: seal entry points, use traps, call exterminator, keep food sealed."},
        {"How to potty train toddler","Potty training: introduce potty, use praise, watch cues, be consistent, stay patient."},
        {"How to remove stains","Stain removal: blot don't rub, treat promptly with cold water, use appropriate cleaner."},
        {"How to apologize effectively","Effective apology: acknowledge wrong, express genuine remorse, and commit to change."},
        {"How to meditate for beginners","Beginner meditation: sit comfortably, focus on breath, observe thoughts, start 5 minutes."},
        {"How to do intermittent fasting","IF 16:8: eat within 8-hour window, fast 16 hours, stay hydrated during fasting period."},
    };

    private static final String[][] SCIENCE = {
        {"What is quantum computing","Quantum computing uses qubits and superposition for exponentially faster certain calculations."},
        {"How does the internet work","The internet uses TCP/IP protocols, routers, DNS, and fiber/wireless to connect devices."},
        {"What is DNA","DNA is a double-helix molecule encoding genetic instructions for development and function."},
        {"How do vaccines work","Vaccines train immune systems using antigens to recognize and fight future infections."},
        {"What is black hole","Black holes are regions where gravity is so strong that nothing, not even light, escapes."},
        {"How does gravity work","Gravity is a fundamental force caused by mass curving spacetime per Einstein's relativity."},
        {"What causes earthquakes","Earthquakes result from tectonic plates sliding, colliding, or separating at fault lines."},
        {"How does evolution work","Evolution occurs via natural selection, genetic mutation, and survival of the fittest."},
        {"What is photosynthesis","Photosynthesis converts sunlight, water, and CO2 into glucose and oxygen in plant cells."},
        {"How do neurons work","Neurons transmit electrical signals via synapses using neurotransmitters across cell networks."},
        {"What is the Big Bang","The Big Bang is the cosmological model describing the universe's origin 13.8 billion years ago."},
        {"How do antibiotics work","Antibiotics kill or inhibit bacterial growth without harming human cells through targeted mechanisms."},
        {"What is stem cell therapy","Stem cells can differentiate into specialized cells, offering treatments for various diseases."},
        {"How does CRISPR work","CRISPR-Cas9 edits DNA by cutting specific gene sequences using guide RNA and protein scissors."},
        {"What is dark matter","Dark matter is undetected mass constituting about 27% of the universe inferred by gravity."},
        {"How do solar panels work","Solar panels convert photons into electrons via photovoltaic cells generating direct current electricity."},
        {"What is nuclear fusion","Nuclear fusion combines light atoms into heavier ones releasing enormous energy, powering the sun."},
        {"How does memory work","Memory forms through synaptic strengthening; hippocampus encodes, and cortex stores long-term memories."},
        {"What is consciousness","Consciousness is subjective awareness; neuroscience links it to neural correlates and integration."},
        {"How do planes fly","Planes fly using wing lift from Bernoulli's principle and angle of attack against airflow."},
    };

    // ── main entry point ────────────────────────────────────────────────────────

    public static List<LocalDocument> load() {
        List<LocalDocument> docs = new ArrayList<>();
        buildFrom(docs, TECH, "https://en.wikipedia.org/wiki/");
        buildFrom(docs, HEALTH, "https://www.healthline.com/");
        buildFrom(docs, FINANCE, "https://www.investopedia.com/");
        buildFrom(docs, LIFESTYLE, "https://www.lifehack.org/");
        buildFrom(docs, EDUCATION, "https://www.coursera.org/");
        buildFrom(docs, ENTERTAINMENT, "https://www.imdb.com/");
        buildFrom(docs, SHOPPING, "https://www.amazon.com/");
        buildFrom(docs, FOOD, "https://www.allrecipes.com/");
        buildFrom(docs, TRAVEL, "https://www.lonelyplanet.com/");
        buildFrom(docs, NEWS_GENERAL, "https://news.google.com/");
        buildFrom(docs, HOWTO, "https://www.wikihow.com/");
        buildFrom(docs, SCIENCE, "https://www.sciencedaily.com/");
        // Expand with variations to exceed 1000 entries
        expandVariants(docs, TECH, "https://en.wikipedia.org/wiki/");
        expandVariants(docs, HEALTH, "https://www.healthline.com/");
        expandVariants(docs, FINANCE, "https://www.investopedia.com/");
        expandVariants(docs, LIFESTYLE, "https://www.lifehack.org/");
        expandVariants(docs, EDUCATION, "https://www.coursera.org/");
        expandVariants(docs, ENTERTAINMENT, "https://www.imdb.com/");
        expandVariants(docs, FOOD, "https://www.allrecipes.com/");
        expandVariants(docs, TRAVEL, "https://www.lonelyplanet.com/");
        expandVariants(docs, HOWTO, "https://www.wikihow.com/");
        expandVariants(docs, SCIENCE, "https://www.sciencedaily.com/");
        System.out.println("SampleDataLoader: loaded " + docs.size() + " documents");
        return docs;
    }

    private static void buildFrom(List<LocalDocument> docs, String[][] data, String baseUrl) {
        for (String[] entry : data) {
            String slug = entry[0].toLowerCase().replace(" ", "-").replace(":", "").replace("/", "");
            add(docs, entry[0], entry[1], baseUrl + slug);
        }
    }

    private static void expandVariants(List<LocalDocument> docs, String[][] data, String baseUrl) {
        String[] prefixes = {"Guide to", "Introduction to", "Beginner's guide to",
                "Advanced", "Complete", "Understanding", "Learn", "Master", "Top tips for"};
        int pi = 0;
        for (String[] entry : data) {
            String prefix = prefixes[pi % prefixes.length];
            String newTitle = prefix + " " + entry[0];
            String newContent = entry[1] + " This guide covers key concepts, best practices, and real-world examples.";
            String slug = newTitle.toLowerCase().replace(" ", "-").replace(":", "").replace("/", "");
            add(docs, newTitle, newContent, baseUrl + "guide/" + slug);
            pi++;
        }
    }
}
