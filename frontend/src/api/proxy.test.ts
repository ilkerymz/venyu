import { describe, expect, it } from "vitest";
import { pickRequestHeaders, pickResponseHeaders, upstreamUrl } from "./proxy";

describe("upstreamUrl", () => {
    it("yol parçalarını ve query string'i backend adresine ekler", () => {
        expect(upstreamUrl("http://localhost:8080", ["events", "by-slug", "abc123"], "?x=1")).toBe(
            "http://localhost:8080/events/by-slug/abc123?x=1",
        );
    });

    it("parçaları yeniden encode eder; çözülmüş '/' yeni bir yol parçası açamaz", () => {
        expect(upstreamUrl("http://api/", ["events", "a/../../me"], "")).toBe(
            "http://api/events/a%2F..%2F..%2Fme",
        );
    });
});

describe("header allowlist", () => {
    it("tarayıcının cookie ve authorization header'larını backend'e geçirmez", () => {
        const picked = pickRequestHeaders(
            new Headers({
                cookie: "session=secret",
                authorization: "Bearer x",
                "content-type": "application/json",
            }),
        );
        expect([...picked.keys()]).toEqual(["content-type"]);
    });

    it("backend'in Set-Cookie header'ını tarayıcıya geçirmez", () => {
        const picked = pickResponseHeaders(
            new Headers({ "set-cookie": "a=b", "content-type": "application/problem+json" }),
        );
        expect([...picked.keys()]).toEqual(["content-type"]);
    });
});
