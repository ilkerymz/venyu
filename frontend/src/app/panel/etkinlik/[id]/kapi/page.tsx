import type { Metadata } from "next";
import { loadAttendees } from "../../../data";
import { DoorScanner } from "./door-scanner";

export const metadata: Metadata = { title: "Kapı", robots: { index: false } };

/** Kapı modu: telefonda kullanılmak üzere; QR okutma + elle kod girişi (ADR-010). */
export default async function DoorPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    const { attendees } = await loadAttendees(id);
    const registered = attendees.filter(
        (a) => a.status === "CONFIRMED" || a.status === "CHECKED_IN",
    ).length;
    const checkedIn = attendees.filter((a) => a.status === "CHECKED_IN").length;
    return <DoorScanner eventId={id} checkedIn={checkedIn} registered={registered} />;
}
