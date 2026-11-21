import type { Metadata } from "next";
import { activeBookingBySlot } from "@/lib/slots";
import { loadBookings, loadOwnedVenue, loadSlots } from "../data";
import { SlotCalendar } from "./slot-calendar";

export const metadata: Metadata = { title: "Mekan takvimi", robots: { index: false } };

export default async function VenueCalendarPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    await loadOwnedVenue(id);
    const [slots, bookings] = await Promise.all([loadSlots(id), loadBookings(id)]);
    return (
        <SlotCalendar
            venueId={id}
            slots={slots}
            activeBookings={Object.fromEntries(activeBookingBySlot(bookings))}
        />
    );
}
