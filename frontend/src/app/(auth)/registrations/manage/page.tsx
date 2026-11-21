import type { Metadata } from "next";
import { Suspense } from "react";
import Link from "next/link";
import { describeError, isApiError } from "@/api/api-error";
import { manage } from "@/api/generated/registration-controller/registration-controller";
import { FormErrors } from "@/components/form-parts";
import { TicketPass } from "@/components/ticket-pass";
import { formatEventDay, formatTimeRange } from "@/lib/format";
import { FormSkeleton } from "../../form-skeleton";
import { AuthHeading } from "../../auth-heading";
import { MissingTokenNotice } from "../../link-problem";
import { CancelManagedRegistration } from "./manage-actions";

export const metadata: Metadata = { title: "Kaydım", robots: { index: false } };

/**
 * Misafirin "yönet" bağlantısı: /registrations/manage?token=… (ADR-010).
 * Görüntüleme yan etkisizdir; sayfa sunucuda yüklenir. İptal ayrı onayla yapılır.
 */
type TokenProps = { searchParams: Promise<{ token?: string }> };

export default function ManageRegistrationPage({ searchParams }: TokenProps) {
    return (
        <Suspense fallback={<FormSkeleton />}>
            <ManageBody searchParams={searchParams} />
        </Suspense>
    );
}

async function ManageBody({ searchParams }: { searchParams: Promise<{ token?: string }> }) {
    const { token } = await searchParams;
    if (!token) {
        return (
            <>
                <AuthHeading title="Kaydım" />
                <MissingTokenNotice retryHref="/" retryLabel="Ana sayfaya dön" />
            </>
        );
    }

    let registration;
    try {
        ({ data: registration } = await manage({ token }));
    } catch (error) {
        const errors =
            isApiError(error, 400) || isApiError(error, 404)
                ? ["Bu bağlantı geçersiz. Biletinle gelen e-postadaki bağlantıyı kullan."]
                : describeError(error);
        return (
            <>
                <AuthHeading title="Kaydım" />
                <FormErrors errors={errors} />
            </>
        );
    }

    const active = registration.status === "CONFIRMED";
    return (
        <>
            <AuthHeading title={registration.eventTitle ?? "Kaydım"}>
                {registration.startsAt &&
                    `${formatEventDay(registration.startsAt)}, ${formatTimeRange(registration.startsAt, registration.endsAt ?? registration.startsAt)}`}
            </AuthHeading>
            <div className="grid gap-6">
                <div className="rounded-xl border bg-card p-6">
                    <TicketPass
                        code={registration.ticketCode}
                        status={registration.status}
                        title={registration.name}
                    />
                </div>
                {registration.eventSlug && (
                    <Link
                        href={`/e/${registration.eventSlug}`}
                        className="text-sm font-medium text-primary underline-offset-4 hover:underline"
                    >
                        Etkinlik sayfasını aç
                    </Link>
                )}
                {active && <CancelManagedRegistration token={token} />}
            </div>
        </>
    );
}
