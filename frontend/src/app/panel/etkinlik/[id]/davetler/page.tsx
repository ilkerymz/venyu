import type { Metadata } from "next";
import { loadEventSummary, loadInvitations } from "../../../data";
import { Invitations } from "./invitations";

export const metadata: Metadata = { title: "Davetler", robots: { index: false } };

export default async function InvitationsPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    const [invitations, event] = await Promise.all([loadInvitations(id), loadEventSummary(id)]);
    const editable = event.status === "DRAFT" || event.status === "PUBLISHED";
    return (
        <Invitations
            eventId={id}
            invitations={invitations}
            editable={editable}
            visibility={event.visibility}
        />
    );
}
