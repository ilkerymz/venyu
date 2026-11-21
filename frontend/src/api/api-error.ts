/**
 * Backend hataları RFC 9457 Problem Details biçiminde döner.
 * Form hatalarında ek olarak `errors` listesi gelir (InvalidFormException,
 * InvalidAnswersException). Spec'te hata şemaları olmadığı için tip elle yazıldı.
 */
export type ProblemDetail = {
    type?: string;
    title?: string;
    status?: number;
    detail?: string;
    instance?: string;
    errors?: string[];
};

export class ApiError extends Error {
    readonly status: number;
    readonly problem: ProblemDetail;

    constructor(status: number, problem: ProblemDetail) {
        super(problem.detail ?? problem.title ?? `İstek başarısız oldu (HTTP ${status}).`);
        this.name = "ApiError";
        this.status = status;
        this.problem = problem;
    }

    /** Kullanıcıya gösterilebilecek hata satırları: önce `errors` listesi, yoksa tek mesaj. */
    get messages(): string[] {
        return this.problem.errors?.length ? this.problem.errors : [this.message];
    }
}

export function isApiError(error: unknown, status?: number): error is ApiError {
    return error instanceof ApiError && (status === undefined || error.status === status);
}

/** Yanıt gövdesini Problem Details olarak okur; JSON değilse (örn. proxy hatası) boş döner. */
export async function readProblem(res: Response): Promise<ProblemDetail> {
    const type = res.headers.get("content-type") ?? "";
    if (!type.includes("json")) return { status: res.status };
    try {
        return (await res.json()) as ProblemDetail;
    } catch {
        return { status: res.status };
    }
}
