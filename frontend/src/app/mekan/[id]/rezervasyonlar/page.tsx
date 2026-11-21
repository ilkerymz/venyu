import type { Metadata } from "next";
import { slotById } from "@/lib/slots";
import { loadBookings, loadOwnedVenue, loadSlots } from "../../data";
import { BookingList } from "./booking-list";

export const metadata: Metadata = { title: "Rezervasyonlar", robots: { index: false } };

export default async function VenueBookingsPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    await loadOwnedVenue(id);
    // Son 30 gün de dahil: yakın geçmişteki rezervasyonların saati de görünsün.
    const [bookings, slots] = await Promise.all([loadBookings(id), loadSlots(id, 30)]);
    return <BookingList bookings={bookings} slots={Object.fromEntries(slotById(slots))} />;
}
