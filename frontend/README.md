# RBU Campus Exam Scheduler — Frontend

React 19 single-page application built with Vite and Material UI.

## Run locally

1. Install Node.js and npm.
2. From this directory, run `npm install`.
3. Start the backend using the instructions in the [project README](../README.md).
4. Run `npm run dev` and open the local URL printed by Vite.

The API defaults to `http://localhost:8080`. To point the frontend at another backend, create a `.env.local` file in this directory with:

```dotenv
VITE_API_BASE_URL=https://your-backend.example.com
```

Do not include database credentials or server secrets in `VITE_*` variables; those values are bundled into the public frontend.

## Features

- Public student schedule lookup by enrollment number.
- Select exams into **My Schedule**, keep selections in the current browser, and export them as an `.ics` calendar.
- Admin login, first-admin setup, dashboard, and department, subject, student, and exam management.
- Admin timetable import for `.xls`, `.xlsx`, and text-based `.pdf` files.

For timetable column requirements, data setup order, and backend configuration, see the backend README.

## Commands

- `npm run dev` — start the development server.
- `npm run lint` — check source lint rules.
- `npm run build` — create the production bundle in `dist/`.
- `npm run preview` — serve the production bundle locally.
