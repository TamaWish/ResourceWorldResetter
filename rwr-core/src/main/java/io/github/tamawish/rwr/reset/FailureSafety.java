package io.github.tamawish.rwr.reset;

/** Classifies whether and how a failed reset may be retried. */
public enum FailureSafety {
  SAFE_TO_RETRY,
  AMBIGUOUS_REVIEW_REQUIRED,
  NOT_RETRYABLE
}
