# Railway Ticket System

[Українська](README.uk.md)

A web application for railway ticket booking and transport management, built with Java 21, Spring Boot, MySQL, React and Material UI.

## Features

- Registration, sign-in, user profiles and role-based access for passengers and administrators.
- Trip search, coach layouts, seat availability and fare calculation.
- Passenger and discount management, booking with a 15-minute payment window, order history and tickets.
- Simulated card payments; no real charges are made.
- Administration of reference data, stations, routes, stops, trains, coaches, seats and scheduled trips.
- Sales, revenue and occupancy analytics, travel recommendations and AI assistants for passengers and administrators.
- English and Ukrainian interface; REST API documentation through Swagger UI.

## Requirements

- JDK 21+.
- Node.js 22.12+ and npm.
- MySQL with a database for the application.
- Google AI API key for AI features (optional).

Maven is included through Maven Wrapper.

## Setup and run

Run the following commands from the project root in PowerShell.

1. Create the local configuration:

   ```powershell
   Copy-Item .env.example .env
   ```

   Set `DATASOURCE_URL`, `DATASOURCE_USERNAME` and `DATASOURCE_PASSWORD`. Replace `JWT_SECRET` with a random secret of at least 32 UTF-8 bytes. Set `GOOGLE_AI_API_KEY` to your key, or leave it empty to run without AI.

2. Prepare the MySQL database. The default startup requires an existing schema. To create tables in a new, empty local database, use this command instead of the regular backend command on the first run:

   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=update"
   ```

   Add `CLIENT` and `ADMIN` to the `role` table. Registration creates clients; administrator access requires an account assigned the `ADMIN` role. Populate the reference data before creating routes and trips. Automated database migrations are not included.

3. Start the backend:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. Start the frontend in a separate terminal:

   ```powershell
   cd frontend
   npm.cmd ci
   npm.cmd run dev
   ```

On Linux/macOS, use `./mvnw` and `npm`. For a different backend address, set `VITE_API_BASE_URL` before starting the frontend; set `FRONTEND_ORIGIN` for a different frontend origin.

- Application: http://localhost:5173
- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
