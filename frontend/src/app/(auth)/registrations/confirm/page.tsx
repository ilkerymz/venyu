import type { Metadata } from "next";
import { Suspense } from "react";
import { FormSkeleton } from "../../form-skeleton";
import { AuthHeading } from "../../auth-heading";
import { MissingTokenNotice } from "../../link-problem";
import { ConfirmRegistrationForm } from "./confirm-form";

export const metadata: Metadata = { title: "Kaydını onayla", robots: { index: false } };

/**
 * Misafir kaydının onay bağlantısı: /registrations/confirm?token=… (ADR-008).
 * Onay butonla yapılır: e-posta tarayıcıları bağlantıyı kendiliğinden açabilir.
 */
type TokenProps = { searchParams: Promise<{ token?: string }> };

export default function ConfirmRegistrationPage({ searchParams }: TokenProps) {
    return (
        <>
            <AuthHeading title="Kaydını onayla">
                Yerin 30 dakika boyunca ayrıldı. Onaylayınca biletin hazır olur.
            </AuthHeading>
            <Suspense fallback={<FormSkeleton />}>
                <ConfirmBody searchParams={searchParams} />
            </Suspense>
        </>
    );
}

async function ConfirmBody({ searchParams }: TokenProps) {
    const { token } = await searchParams;
    return token ? (
        <ConfirmRegistrationForm token={token} />
    ) : (
        <MissingTokenNotice retryHref="/" retryLabel="Ana sayfaya dön" />
    );
}
