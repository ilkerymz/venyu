import type { Metadata } from "next";
import { Suspense } from "react";
import { describeError, isApiError } from "@/api/api-error";
import { preview } from "@/api/generated/invitation-controller/invitation-controller";
import { hasSession } from "@/auth/server";
import { FormErrors, Notice } from "@/components/form-parts";
import { RegistrationPanel } from "@/components/registration-panel";
import { formatEventDay, formatTimeRange } from "@/lib/format";
import { AuthHeading } from "../../auth-heading";
import { FormSkeleton } from "../../form-skeleton";
import { MissingTokenNotice } from "../../link-problem";
import { acceptInvitationAction } from "../actions";

export const metadata: Metadata = { title: "Davet", robots: { index: false } };

type TokenProps = { searchParams: Promise<{ token?: string }> };

/** Organizatörün davet e-postasındaki bağlantı: /invitations/accept?token=… (ADR-008). */
export default function AcceptInvitationPage({ searchParams }: TokenProps) {
    return (
        <Suspense fallback={<FormSkeleton />}>
            <AcceptBody searchParams={searchParams} />
        </Suspense>
    );
}

async function AcceptBody({ searchParams }: TokenProps) {
    const { token } = await searchParams;
    if (!token) {
        return (
            <>
                <AuthHeading title="Davet" />
                <MissingTokenNotice retryHref="/" retryLabel="Ana sayfaya dön" />
            </>
        );
    }

    let invitation;
    try {
        ({ data: invitation } = await preview({ token }));
    } catch (error) {
        const errors =
            isApiError(error, 400) || isApiError(error, 404)
                ? [
                      "Bu davet bağlantısı geçersiz. E-postadaki bağlantının tamamını kullandığından emin ol.",
                  ]
                : describeError(error);
        return (
            <>
                <AuthHeading title="Davet" />
                <FormErrors errors={errors} />
            </>
        );
    }

    const when =
        invitation.startsAt &&
        `${formatEventDay(invitation.startsAt)}, ${formatTimeRange(invitation.startsAt, invitation.endsAt ?? invitation.startsAt)}`;

    if (invitation.status === "ACCEPTED") {
        return (
            <>
                <AuthHeading title={invitation.eventTitle ?? "Davet"}>{when}</AuthHeading>
                <Notice>Bu davet daha önce kabul edildi. Biletin e-postanda.</Notice>
            </>
        );
    }

    const loginHref = `/giris?next=${encodeURIComponent(`/invitations/accept?token=${token}`)}`;
    return (
        <>
            <AuthHeading title={invitation.eventTitle ?? "Davet"}>
                {when}
                {when && <br />}
                Bu etkinliğe {invitation.email} adresiyle davet edildin.
            </AuthHeading>
            <div className="rounded-xl border bg-card p-6">
                <RegistrationPanel
                    action={acceptInvitationAction.bind(null, token)}
                    guestFields="name"
                    submitLabel="Daveti kabul et"
                    tickets={invitation.tickets ?? []}
                    fields={invitation.formFields ?? []}
                    isMember={await hasSession()}
                    requiresAccount={invitation.requiresAccount ?? false}
                    loginHref={loginHref}
                />
            </div>
        </>
    );
}
