import type { Metadata } from "next";
import { AuthHeading } from "../../auth-heading";
import { MissingTokenNotice } from "../../link-problem";
import { ConfirmRegistrationForm } from "./confirm-form";

export const metadata: Metadata = { title: "Kaydını onayla", robots: { index: false } };

/**
 * Misafir kaydının onay bağlantısı: /registrations/confirm?token=… (ADR-008).
 * Onay butonla yapılır: e-posta tarayıcıları bağlantıyı kendiliğinden açabilir.
 */
export default async function ConfirmRegistrationPage({
    searchParams,
}: {
    searchParams: Promise<{ token?: string }>;
}) {
    const { token } = await searchParams;
    return (
        <>
            <AuthHeading title="Kaydını onayla">
                {token
                    ? "Yerin 30 dakika boyunca ayrıldı. Onaylayınca biletin hazır olur."
                    : undefined}
            </AuthHeading>
            {token ? (
                <ConfirmRegistrationForm token={token} />
            ) : (
                <MissingTokenNotice retryHref="/" retryLabel="Ana sayfaya dön" />
            )}
        </>
    );
}
