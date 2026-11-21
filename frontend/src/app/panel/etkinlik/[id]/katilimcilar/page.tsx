import type { Metadata } from "next";
import { loadAttendees, loadEventSummary } from "../../../data";
import { AttendeeList } from "./attendee-list";

export const metadata: Metadata = { title: "Katılımcılar", robots: { index: false } };

export default async function AttendeesPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    const [{ attendees, fields }, event] = await Promise.all([
        loadAttendees(id),
        loadEventSummary(id),
    ]);
    return <AttendeeList attendees={attendees} fields={fields} title={event.title ?? "Etkinlik"} />;
}
