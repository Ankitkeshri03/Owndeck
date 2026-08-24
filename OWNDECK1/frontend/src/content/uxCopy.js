export const voiceAndStyleGuide = {
  tone: "Clear, human, confident, and action-oriented.",
  principles: [
    "Lead with user intent and next action.",
    "Use plain language and short sentences.",
    "Keep status and error wording specific.",
    "Stay consistent with product terms.",
  ],
  canonicalTerms: {
    invoice: "invoice",
    warrantyExpiry: "warranty expiry",
    expiringSoon: "expiring soon",
    complaintEmail: "complaint email",
  },
};

export const navCopy = {
  brandTagline: "Invoice and warranty expiry manager",
  links: {
    home: "Home",
    dashboard: "Dashboard",
    upload: "Upload Invoice",
    filters: "Expiring Soon",
    features: "Features",
    howItWorks: "How It Works",
    login: "Sign In",
    signup: "Create Account",
    logout: "Sign Out",
  },
  greetingPrefix: "Hello",
};

export const homeCopy = {
  badge: "OwnDeck - React + Spring Boot",
  heroTitle: "Track every invoice. Never miss warranty expiry.",
  heroSubtitle:
    "Upload invoices, extract product details with OCR + AI, monitor warranty expiry in one dashboard, and get browser reminders before coverage runs out.",
  ctaPrimary: "Create account",
  ctaSecondary: "Sign in",
  ctaTertiary: "View features",
  benefitsTitle: "What you can do",
  benefitCards: [
    {
      title: "Extract invoice details",
      body: "Capture product name, quantity, purchase date, warranty period, and warranty expiry.",
    },
    {
      title: "Get timely expiry alerts",
      body: "Get popup reminders inside the app and browser notifications when products are close to expiry.",
    },
    {
      title: "Generate a complaint email",
      body: "Create a professional complaint email for each product issue.",
    },
  ],
  featuresHeading: "Built for warranty management",
  featuresSubheading:
    "Use the navbar to move between upload, dashboard, and product details.",
  featuresCta: "Start now",
  featurePanels: [
    {
      title: "Invoice upload",
      body: "Upload JPG, PNG, WEBP, or PDF files with preview support.",
    },
    {
      title: "Warranty dashboard",
      body: "Filter products by Active, Expiring Soon, and Expired.",
    },
    {
      title: "Secure account access",
      body: "JWT authentication ensures each user sees only their own records.",
    },
  ],
  howTitle: "How it works",
  howSubtitle:
    "A simple workflow from invoice upload to warranty expiry tracking and complaint support.",
  howSteps: [
    { title: "Upload invoice", body: "Add an image or PDF invoice up to 10MB." },
    {
      title: "Review extracted details",
      body: "Confirm quantity, purchase date, warranty period, and warranty expiry.",
    },
    {
      title: "Track and resolve",
      body: "Monitor expiry status and generate a complaint email when needed.",
    },
  ],
  footerTitle: "Ready to organize your purchases?",
  footerSubtitle: "Start now and keep every warranty expiry date visible.",
  footerPrimary: "Create account",
  footerSecondary: "I already have an account",
};

export const authCopy = {
  login: {
    title: "Sign in to OwnDeck",
    subtitle: "Access your invoices and warranty expiry dashboard.",
    emailLabel: "Email address",
    passwordLabel: "Password",
    submit: "Sign In",
    footerPrefix: "New to OwnDeck?",
    footerLink: "Create account",
    loading: "Signing you in...",
  },
  signup: {
    title: "Create your OwnDeck account",
    subtitle: "Start tracking invoices and warranty expiry in one place.",
    nameLabel: "Full name",
    emailLabel: "Email address",
    phoneLabel: "Phone number (optional)",
    passwordLabel: "Password",
    submit: "Create Account",
    footerPrefix: "Already have an account?",
    footerLink: "Sign in",
    loading: "Creating your account...",
  },
  validation: {
    requiredName: "Enter your full name.",
    requiredEmail: "Enter your email address.",
    invalidEmail: "Enter a valid email address.",
    invalidAlertEmail: "Enter a valid alert email address.",
    requiredPassword: "Enter your password.",
    shortPassword: "Password must be at least 6 characters.",
  },
};

export const dashboardCopy = {
  loading: "Loading your warranty dashboard...",
  heading: "Warranty Dashboard",
  subheading: "Track product status, warranty expiry dates, and get in-app reminder popups without email.",
  uploadCta: "Upload Invoice",
  alertsModal: {
    title: "Warranty expiry alerts",
    close: "Close",
    empty: "No products are currently near warranty expiry.",
    line: "{message} Warranty expiry on {date}.",
  },
  alertsPanel: {
    title: "Reminder Center",
    subtitle: "OwnDeck checks alerts while the dashboard is open and shows popups inside the app.",
    enable: "Enable Browser Alerts",
    enabled: "In-app reminder popups are active",
    unsupported: "In-app reminder popups stay available while the dashboard is open.",
    denied: "In-app reminder popups stay available while the dashboard is open.",
    refresh: "Check Alerts Now",
    pollNote: "OwnDeck refreshes reminders automatically while the dashboard is open.",
    activeBadge: "{count} active reminder(s)",
  },
  summary: {
    total: "Total Products",
    active: "Active",
    expiringSoon: "Expiring Soon",
    expired: "Expired",
  },
  filters: {
    searchPlaceholder: "Search by product name or description",
    allStatus: "All Status",
    active: "Active",
    expiringSoon: "Expiring Soon",
    expired: "Expired",
    quickFilter: "Show Expiring Soon",
  },
  card: {
    description: "Description",
    quantity: "Quantity",
    purchaseDate: "Purchase Date",
    expiryDate: "Warranty Expiry",
    daysRemaining: "Days remaining",
  },
  emptyState:
    "No products found for this filter. Upload an invoice or adjust your search.",
};

export const uploadCopy = {
  pageTitle: "Upload Invoice",
  pageSubtitle:
    "Upload an invoice to extract product details and calculate warranty expiry.",
  sectionTitle: "Upload and extract",
  sectionSubtitle: "Accepted formats: JPG, PNG, WEBP, PDF (max 10MB)",
  chooseFile: "Choose invoice file",
  takePhoto: "Take photo with camera",
  takePhotoHint:
    "On mobile, this opens the device camera if the browser supports it. If live camera preview does not start, use this option instead.",
  selected: "Selected file",
  pdfPreviewNote:
    "PDF preview depends on browser support. OCR extraction runs on the backend.",
  manualTitle: "Manual details (optional)",
  manualFields: {
    productName: "Product name",
    description: "Product description",
    quantity: "Quantity",
    purchaseDate: "Purchase date",
    warrantyMonths: "Warranty period (months)",
    expiryDate: "Warranty expiry date",
  },
  submit: "Upload and Extract",
  submitting: "Uploading invoice...",
  reset: "Reset",
  extractedTitle: "Extracted product details",
  extractedEmpty:
    "Upload an invoice to review extracted product details and warranty expiry status.",
  openDetail: "Open Product Details",
  detailLabels: {
    name: "Product Name",
    description: "Description",
    quantity: "Quantity",
    purchaseDate: "Purchase Date",
    expiryDate: "Warranty Expiry",
    status: "Status",
    daysRemaining: "Days Remaining",
  },
};

export const productCopy = {
  loading: "Loading product details...",
  notFound: "We couldn't find this product.",
  statusLabel: "Status",
  labels: {
    description: "Description",
    quantity: "Quantity",
    purchaseDate: "Purchase Date",
    expiryDate: "Warranty Expiry",
    warrantyDuration: "Warranty Period",
    daysRemaining: "Days Remaining",
  },
  invoiceButtons: {
    view: "View Invoice",
    download: "Download Invoice",
  },
  complaint: {
    title: "Complaint Email Generator",
    subtitle:
      "Write a short prompt about your issue, and OwnDeck will turn it into a full professional email.",
    issueLabel: "Simple issue prompt",
    generate: "Generate Complaint Email",
    generating: "Generating complaint email...",
    copy: "Copy Email",
    outputSubject: "Subject",
    outputBody: "Complaint Email",
  },
};

export const messageLibrary = {
  loading: {
    page: "Loading data...",
    auth: "Checking your account...",
  },
  success: {
    invoiceSelected: "Invoice selected. Review details, then upload.",
    invoiceUploaded: "Invoice uploaded. Product details were extracted successfully.",
    complaintGenerated: "Complaint email generated successfully.",
    complaintCopied: "Complaint email copied to clipboard.",
    notificationsEnabled: "Browser notifications enabled.",
    signedOut: "You have been signed out.",
  },
  errors: {
    generic: "Something went wrong. Please try again.",
    network: "Unable to reach the backend API. Make sure the Spring Boot server is running on http://localhost:8081 and try again.",
    timeout: "The request took too long. Please try again.",
    unauthorized: "Your session expired. Please sign in again.",
    authLoginFailed: "Sign-in failed. Check your email and password.",
    authSignupFailed: "Account creation failed. Please review your details and try again.",
    dashboardLoadFailed: "Unable to load your dashboard right now.",
    productLoadFailed: "Unable to load product details.",
    uploadFailed: "Invoice upload failed. Please try again.",
    complaintFailed: "Unable to generate complaint email.",
    noFile: "Select an invoice file before uploading.",
    invalidFileType: "Unsupported file type. Upload JPG, PNG, WEBP, or PDF.",
    fileTooLarge: "File is too large. Maximum allowed size is 10MB.",
    invalidQuantity: "Quantity must be at least 1.",
    invalidWarrantyMonths: "Warranty period cannot be negative.",
    missingIssue: "Describe the issue before generating a complaint email.",
    notificationPermissionDenied: "Browser notifications are blocked for this site.",
  },
  empty: {
    dashboard: "No products yet. Upload your first invoice to start tracking warranty expiry.",
    alerts: "No warranty expiry alerts right now.",
  },
};
