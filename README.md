# CV Optimizer

An AI-powered resume enhancement tool that automatically rewrites and formats CVs for technical roles using Claude AI. Upload a PDF, DOCX, or TXT file and get back a professionally formatted, ATS-optimized PDF.

## Features

- **Multi-format input** — accepts PDF, DOCX, and plain text files (up to 5 MB)
- **AI enhancement** — rewrites bullet points with strong action verbs, adds quantified metrics, and generates a focused professional summary
- **Skill categorization** — organizes skills into Languages, Frameworks, Databases, Cloud & DevOps, and Tools
- **Target role focus** — optionally specify a role to tailor the enhancements
- **PDF output** — generates a modern, ATS-friendly PDF with navy/blue color scheme
- **Privacy-first** — uploaded files are processed in memory and never persisted

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2.0, Java 17 |
| AI | Anthropic Claude API (`claude-sonnet-4-6`) |
| PDF parsing | Apache PDFBox 2.0.29 |
| DOCX parsing | Apache POI 5.2.5 |
| PDF generation | iText 7.2.5 |
| Frontend | Thymeleaf, Bootstrap 5.3.2 |
| Build | Maven |

## Prerequisites

- Java 17+
- Maven 3.8+
- An [Anthropic API key](https://console.anthropic.com/)

## Getting Started

### 1. Clone the repository

```bash
git clone <repo-url>
cd cv-optimizer
```

### 2. Set your API key

```bash
export CLAUDE_API_KEY=sk-ant-...
```

On Windows:

```cmd
set CLAUDE_API_KEY=sk-ant-...
```

### 3. Build and run

```bash
mvn spring-boot:run
```

Or build a JAR first:

```bash
mvn package -DskipTests
java -jar target/cv-optimizer-*.jar
```

On Windows you can also use the included launcher:

```cmd
run.bat
```

### 4. Open the app

Navigate to [http://localhost:8087](http://localhost:8087) in your browser.

## Configuration

All settings are in `src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `server.port` | `8087` | HTTP port |
| `claude.api.key` | `${CLAUDE_API_KEY}` | Anthropic API key (use env var) |
| `claude.api.model` | `claude-sonnet-4-6` | Claude model to use |
| `spring.servlet.multipart.max-file-size` | `5MB` | Max upload size |

## Project Structure

```
src/main/
├── java/com/cvoptimizer/
│   ├── CvOptimizerApplication.java   # Entry point
│   ├── controller/
│   │   └── CvController.java         # HTTP endpoints (GET /, POST /optimize)
│   ├── model/
│   │   └── CvData.java               # CV data model
│   └── service/
│       ├── CvParserService.java      # Text extraction from PDF/DOCX/TXT
│       ├── CvEnhancerService.java    # Claude API integration
│       └── PdfGeneratorService.java  # iText PDF generation
└── resources/
    ├── application.properties
    └── templates/
        └── index.html                # Web UI
```

## How It Works

1. User uploads a CV file with an optional target role.
2. `CvParserService` extracts raw text from the file.
3. `CvEnhancerService` sends the text to Claude with a structured prompt.
4. Claude returns a JSON object with enhanced sections (summary, skills, experience, etc.).
5. `PdfGeneratorService` renders the structured data into a formatted PDF.
6. The PDF is returned as a file download.

## API

### `POST /optimize`

Accepts `multipart/form-data`.

| Field | Type | Required | Description |
|---|---|---|---|
| `cvFile` | File | Yes | PDF, DOCX, or TXT file |
| `targetRole` | String | No | Role to tailor the CV toward |

Returns the optimized CV as a PDF file download, or a JSON error object on failure.