package co.sena.academic.adapter.out.actors;

/** Supplies the short-lived M2M token used only for internal Actors calls. */
public interface ServiceAccessTokenProvider {
    String accessToken();
}
