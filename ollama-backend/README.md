# English Tutor Ollama Proxy Backend

This is a lightweight Python FastAPI server designed to run alongside a self-hosted Ollama instance. It serves as a secure proxy backend for the **English Tutor** mobile application.

## Setup & Deployment Instructions

### Prerequisites
- A VPS (e.g., Hetzner, DigitalOcean, AWS) with at least 4 vCPUs and 8GB of RAM (minimum required to run an 8B parameter model smoothly).
- Docker and Docker Compose installed on the VPS.

---

### Step 1: Deploy with Docker Compose

1. Clone or copy the files in this directory (`main.py`, `Dockerfile`, `docker-compose.yml`, `requirements.txt`) to a folder on your VPS.
2. Run the services in background mode:
   ```bash
   docker-compose up -d
   ```

This spins up two services:
- **Ollama**: running at `http://localhost:11434`
- **FastAPI Proxy**: running at `http://localhost:8000`

---

### Step 2: Download the Llama 3.1 8B Model

Ollama starts with an empty model registry. You need to pull the recommended Llama model so it is available to the API:

```bash
docker exec -it ollama-service ollama pull llama3.1:8b
```

---

### Step 3: Environment Variables (Optional Customization)

You can customize behavior by editing the `environment` section in the `docker-compose.yml` file:

- `API_KEY`: Sets the security token. The mobile app must supply this key as the `X-API-Key` header on every request (configure this in the App Settings screen).
- `OLLAMA_MODEL`: Change this if you wish to run a different model (e.g. `gemma2:9b` or `llama3.2:3b`).
- `OLLAMA_HOST`: Set to `http://ollama:11434` so the FastAPI container can communicate with the Ollama service.

---

### Step 4: Connecting the Mobile App

1. Open the **English Tutor** app on your Android device.
2. Go to the **Settings** screen (the third tab).
3. Under **Tutor Engine Service**, select **Custom Ollama VPS Backend**.
4. Enter your connection details:
   - **Ollama Proxy URL**: `http://YOUR_VPS_IP:8000` (FastAPI port)
   - **X-API-Key**: The exact string matching the `API_KEY` defined on your VPS.
   - **Model Name**: `llama3.1:8b` (or your customized model name).
5. Click **Save VPS Config**.
6. Navigate back to the **Tutor** tab, select a level and topic, and start practicing! Your conversations and corrections will now stream entirely from your own self-hosted VPS!
