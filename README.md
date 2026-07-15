# English Tutor App

A free, unlimited English conversation practice app. Chat by text or voice with an AI tutor that corrects your mistakes and keeps the conversation going — powered by a self-hosted open-source LLM (Ollama), so there's no per-token billing and no artificial daily usage cap.

## Why self-hosted instead of a paid API?

Most AI language-learning apps rely on OpenAI/Gemini/Claude APIs, which means either a paywall or a daily token quota. This project runs its own LLM (via [Ollama](https://ollama.com)) on a personal VPS — free to use as much as you want, limited only by the server's own hardware, not by billing.

## Architecture

```
Flutter App (iOS/Android)
    │  HTTPS + SSE streaming
    ▼
FastAPI Backend (VPS)
    │  local HTTP calls
    ▼
Ollama (same VPS, self-hosted LLM)
```

## Features

- **Text & voice chat** — speak or type, the AI responds in kind (speech-to-text input, text-to-speech playback)
- **Adjustable difficulty** — Beginner / Intermediate / Advanced
- **Topic modes** — Free talk, Job interview, Travel, Daily life, Business
- **Grammar correction** — inline correction chips with explanations, without breaking conversation flow
- **Local history** — all conversations saved on-device, no account required
- **Streaming responses** — AI replies appear live, token by token

## Tech Stack

| Layer | Tech |
|---|---|
| Mobile app | Flutter (iOS + Android) |
| State management | Provider / Riverpod |
| Voice input | `speech_to_text` |
| Voice output | `flutter_tts` |
| Local storage | `sqflite` |
| Secure storage | `flutter_secure_storage` |
| Backend | Python, FastAPI |
| LLM | Ollama (`llama3.1:8b` or `llama3.2:3b`) |
| Deployment | Docker Compose on a VPS (Hetzner/OVH) |

## Project Structure

```
english-tutor-app/
├── backend/
│   ├── main.py              # FastAPI app: /chat, /correct, /health
│   ├── requirements.txt
│   ├── docker-compose.yml   # Ollama + FastAPI together
│   └── README.md            # backend-specific setup
├── mobile/
│   ├── lib/
│   │   ├── screens/         # Home, Chat, History, Settings
│   │   ├── services/        # API client, TTS/STT wrappers, local DB
│   │   ├── models/
│   │   └── main.dart
│   └── pubspec.yaml
└── README.md                 # this file
```

## Setup

### 1. Backend + Ollama (local dev)

```bash
ollama pull llama3.1:8b
ollama list          # confirm it's installed
```

Test Ollama directly:
```bash
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.1:8b",
  "prompt": "Say hello in English",
  "stream": false
}'
```

Run the backend:
```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

Set environment variables before running:
```bash
export API_KEY=your-secret-key
export OLLAMA_MODEL=llama3.1:8b
```

### 2. Mobile app

```bash
cd mobile
flutter pub get
flutter run
```

In the app's **Settings** screen, set the backend URL:
- Testing on the same PC's emulator: `http://10.0.2.2:8000` (Android emulator) or `http://localhost:8000` (iOS simulator)
- Testing on a physical phone (same wifi): your PC's LAN IP, e.g. `http://192.168.1.42:8000` (find it with `ipconfig` on Windows)
- Production: your VPS's domain/IP

And the API key matching `API_KEY` from the backend `.env`.

### 3. Production deployment (VPS)

```bash
cd backend
docker-compose up -d
```

This starts Ollama and the FastAPI backend together. Make sure to:
- Pull the model inside the container on first run (`docker exec -it <ollama_container> ollama pull llama3.1:8b`)
- Set a strong `API_KEY`
- Put a reverse proxy (Caddy/Nginx) with HTTPS in front of the FastAPI backend before exposing it publicly

## Roadmap / Ideas

- [ ] Pronunciation scoring
- [ ] Daily streak / practice reminders (optional, kept non-gamified)
- [ ] Export conversation history
- [ ] Multi-language support beyond English

## License

Personal/portfolio project.

<img width="1440" height="2560" alt="image" src="https://github.com/user-attachments/assets/4df63612-9362-48ec-82b5-aee9c95de317" />
<img width="2560" height="1440" alt="image" src="https://github.com/user-attachments/assets/cc24529c-1f50-4e0c-bd3f-73b1977073ea" />
<img width="2176" height="1632" alt="image" src="https://github.com/user-attachments/assets/07f69964-b4ee-4652-9b28-a290b03a1bad" />
<img width="2176" height="1632" alt="image" src="https://github.com/user-attachments/assets/b328e209-5731-432d-b143-cf5c997d8807" />
<img width="2176" height="1632" alt="image" src="https://github.com/user-attachments/assets/65321f6a-1581-4427-be47-eef4694aaf36" />
