import type { Metadata } from "next";
import { Suspense } from "react";
import { FormSkeleton } from "../form-skeleton";
import { AuthHeading } from "../auth-heading";
import { MissingTokenNotice } from "../link-problem";
import { VerifyEmailForm } from "./verify-form";

export const metadata: Metadata = { title: "E-postanı doğrula", robots: { index: false } };

/**
 * Backend'in doğrulama e-postasındaki bağlantı: /verify-email?token=… (ADR-004).
 * Doğrulama sayfa açılınca değil butonla yapılır: e-posta güvenlik tarayıcıları bağlantıları
 * kendiliğinden açabilir; yan etkili işlem kullanıcının açık eylemiyle (POST) olmalı.
 */
type TokenProps = { searchParams: Promise<{ token?: string }> };

export default function VerifyEmailPage({ searchParams }: TokenProps) {
    return (
        <>
            <AuthHeading title="E-postanı doğrula">
                Doğrulamayı tamamlamak için butona bas.
            </AuthHeading>
            <Suspense fallback={<FormSkeleton />}>
                <VerifyBody searchParams={searchParams} />
            </Suspense>
        </>
    );
}

async function VerifyBody({ searchParams }: TokenProps) {
    const { token } = await searchParams;
    return token ? (
        <VerifyEmailForm token={token} />
    ) : (
        <MissingTokenNotice retryHref="/hesabim" retryLabel="Hesabıma git" />
    );
}
