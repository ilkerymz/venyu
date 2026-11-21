import type { Metadata } from "next";
import { AuthHeading } from "../auth-heading";
import { MissingTokenNotice } from "../link-problem";
import { VerifyEmailForm } from "./verify-form";

export const metadata: Metadata = { title: "E-postanı doğrula", robots: { index: false } };

/**
 * Backend'in doğrulama e-postasındaki bağlantı: /verify-email?token=… (ADR-004).
 * Doğrulama sayfa açılınca değil butonla yapılır: e-posta güvenlik tarayıcıları bağlantıları
 * kendiliğinden açabilir; yan etkili işlem kullanıcının açık eylemiyle (POST) olmalı.
 */
export default async function VerifyEmailPage({
    searchParams,
}: {
    searchParams: Promise<{ token?: string }>;
}) {
    const { token } = await searchParams;
    return (
        <>
            <AuthHeading title="E-postanı doğrula">
                {token ? "Doğrulamayı tamamlamak için butona bas." : undefined}
            </AuthHeading>
            {token ? (
                <VerifyEmailForm token={token} />
            ) : (
                <MissingTokenNotice retryHref="/hesabim" retryLabel="Hesabıma git" />
            )}
        </>
    );
}
