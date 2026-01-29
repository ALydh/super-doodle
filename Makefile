.PHONY: install dev dev-frontend dev-backend test test-frontend test-backend lint lint-frontend lint-backend build build-frontend build-backend clean help

FRONTEND_DIR = frontend
BACKEND_DIR = backend

help:
	@echo "Available targets:"
	@echo "  install        Install all dependencies"
	@echo "  dev            Start frontend and backend in dev mode"
	@echo "  dev-frontend   Start frontend dev server only"
	@echo "  dev-backend    Start backend dev server only"
	@echo "  test           Run all tests"
	@echo "  test-frontend  Run frontend E2E tests"
	@echo "  test-backend   Run backend tests"
	@echo "  lint           Run all linters"
	@echo "  lint-frontend  Run frontend linter"
	@echo "  lint-backend   Run backend compile check"
	@echo "  build          Production build for both"
	@echo "  build-frontend Build frontend for production"
	@echo "  build-backend  Build backend JAR"
	@echo "  clean          Remove build artifacts and reset database"
	@echo "  fetch-data     Download fresh Wahapedia CSV data"

install:
	cd $(FRONTEND_DIR) && npm install
	cd $(FRONTEND_DIR) && npx playwright install
	cd $(BACKEND_DIR) && nice -n 19 sbt compile

dev:
	@echo "Starting frontend and backend..."
	@echo "Frontend: http://localhost:5173"
	@echo "Backend:  http://localhost:8080"
	@echo ""
	@$(MAKE) -j2 dev-frontend dev-backend

dev-frontend:
	cd $(FRONTEND_DIR) && npm run dev

dev-backend:
	cd $(BACKEND_DIR) && nice -n 19 sbt run

test: test-frontend test-backend

test-frontend:
	cd $(FRONTEND_DIR) && npm run test:e2e

test-backend:
	cd $(BACKEND_DIR) && nice -n 19 sbt test

lint: lint-frontend lint-backend

lint-frontend:
	cd $(FRONTEND_DIR) && npm run lint

lint-backend:
	cd $(BACKEND_DIR) && nice -n 19 sbt compile

build: build-frontend build-backend

build-frontend:
	cd $(FRONTEND_DIR) && npm run build

build-backend:
	cd $(BACKEND_DIR) && nice -n 19 sbt assembly

clean:
	rm -rf $(FRONTEND_DIR)/dist
	rm -rf $(FRONTEND_DIR)/node_modules/.vite
	rm -rf $(BACKEND_DIR)/target
	rm -rf $(BACKEND_DIR)/project/target
	rm -f $(BACKEND_DIR)/wahapedia.db
	@echo "Cleaned build artifacts and reset database"

fetch-data:
	./scripts/fetch-wahapedia.sh
