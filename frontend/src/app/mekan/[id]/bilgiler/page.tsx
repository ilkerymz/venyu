import type { Metadata } from "next";
import { updateVenueAction } from "../../actions";
import { loadOwnedVenue } from "../../data";
import { VenueForm } from "../../venue-form";

export const metadata: Metadata = { title: "Mekan bilgileri", robots: { index: false } };

export default async function VenueInfoPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    const venue = await loadOwnedVenue(id);
    return (
        <VenueForm
            action={updateVenueAction.bind(null, id)}
            venue={venue}
            submitLabel="Değişiklikleri kaydet"
        />
    );
}
