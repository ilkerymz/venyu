"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { describeError } from "@/api/api-error";
import {
    confirmPasswordReset,
    login,
    logout,
    register1 as register,
    requestPasswordReset,
    verifyEmail,
} from "@/api/generated/auth-controller/auth-controller";
import { becomeOrganizer } from "@/api/generated/me-controller/me-controller";
import type { TokenResponse } from "@/api/generated/model";
import { refreshSession } from "./refresh";
import { authHeaders } from "./server";
import {
    REFRESH_COOKIE,
    type SessionTokens,
    clearSession,
    safeNextPath,
    writeSession,
} from "./session";

/**
 * Giriş ve hesap işlemleri Server Action olarak çalışır: form doğrudan Next.js sunucusuna
 * POST eder, token'lar cookie'ye sunucuda yazılır, tarayıcı token'ı hiç görmez.
 * Next.js Server Action'larda Origin kontrolü yapar (CSRF).
 */
export type FormState = {
    errors?: string[];
    done?: boolean;
    /** React form action'dan sonra alanları sıfırlar; hata durumunda yazılanlar geri doldurulur (şifre hariç). */
    values?: Record<string, string>;
};

const text = (form: FormData, name: string) => String(form.get(name) ?? "").trim();

function toSession(data: TokenResponse): SessionTokens {
    if (!data.accessToken || !data.refreshToken || !data.expiresIn) {
        throw new Error("Token yanıtı eksik.");
    }
    return {
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        expiresIn: data.expiresIn,
    };
}

async function startSession(email: string, password: string) {
    const { data } = await login({ email, password });
    writeSession(await cookies(), toSession(data));
}

export async function loginAction(_: FormState, form: FormData): Promise<FormState> {
    try {
        await startSession(text(form, "email"), String(form.get("password") ?? ""));
    } catch (error) {
        return { errors: describeError(error), values: { email: text(form, "email") } };
    }
    redirect(safeNextPath(form.get("next")));
}

export async function registerAction(_: FormState, form: FormData): Promise<FormState> {
    const email = text(form, "email");
    const password = String(form.get("password") ?? "");
    try {
        await register({ displayName: text(form, "displayName"), email, password });
        await startSession(email, password);
    } catch (error) {
        return {
            errors: describeError(error),
            values: { displayName: text(form, "displayName"), email },
        };
    }
    redirect("/hesabim?hosgeldin=1");
}

export async function logoutAction(): Promise<void> {
    const store = await cookies();
    const refreshToken = store.get(REFRESH_COOKIE)?.value;
    if (refreshToken) {
        // Backend kapalı olsa bile çıkış tarayıcıda tamamlanır; refresh token süresiyle düşer.
        await logout({ refreshToken }).catch(() => undefined);
    }
    clearSession(store);
    redirect("/");
}

export async function requestPasswordResetAction(_: FormState, form: FormData): Promise<FormState> {
    try {
        // Backend hesap olsun olmasın 202 döner; e-postanın kayıtlı olup olmadığı sızmaz.
        await requestPasswordReset({ email: text(form, "email") });
    } catch (error) {
        return { errors: describeError(error), values: { email: text(form, "email") } };
    }
    return { done: true };
}

export async function confirmPasswordResetAction(_: FormState, form: FormData): Promise<FormState> {
    try {
        await confirmPasswordReset({
            token: text(form, "token"),
            newPassword: String(form.get("newPassword") ?? ""),
        });
    } catch (error) {
        return { errors: describeError(error) };
    }
    // Backend sıfırlamada bütün oturumları kapatır; bu tarayıcıdakini de temizle.
    clearSession(await cookies());
    redirect("/giris?sifre=yenilendi");
}

export async function verifyEmailAction(_: FormState, form: FormData): Promise<FormState> {
    try {
        await verifyEmail({ token: text(form, "token") });
    } catch (error) {
        return { errors: describeError(error) };
    }
    return { done: true };
}

export async function becomeOrganizerAction(): Promise<FormState> {
    const store = await cookies();
    try {
        await becomeOrganizer({ headers: await authHeaders() });
        // Roller token'da taşınır: yeni rolün geçerli olması için token'ları hemen yenile.
        const refreshToken = store.get(REFRESH_COOKIE)?.value;
        const result = refreshToken ? await refreshSession(refreshToken) : null;
        if (result?.kind === "ok") writeSession(store, result.tokens);
    } catch (error) {
        return { errors: describeError(error) };
    }
    redirect("/panel");
}
