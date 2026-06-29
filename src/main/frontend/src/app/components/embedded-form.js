'use client'

import { useEffect, useRef } from "react";
import { Form } from '@bpmn-io/form-js-viewer';

export default function EmbeddedForm({ schema, data, onComplete, submitting }) {
  const formContainer = useRef();
  const form = useRef();
  const containsSubmit = schema?.components?.some(
    comp => comp.type === 'button' && comp.action === 'submit'
  );

  const submit = () => {
    form.current?.submit();
  };

  useEffect(() => {
    form.current = new Form({
      container: formContainer.current
    });

    form.current.importSchema(schema, data);
    form.current.on('submit', (event) => {
      onComplete(event.data);
    });

    return () => {
      form.current?.destroy?.();
      form.current = null;
    };
  }, [schema, data, onComplete]);

  return (
    <div className="cf-wrapper">
      <div ref={formContainer} />

      {!containsSubmit && (
        <div className="cf-footer">
          <button
            className="cf-submit-btn"
            onClick={submit}
            disabled={submitting}
          >
            {submitting ? 'در حال ارسال...' : 'ارسال برای بررسی'}
          </button>
        </div>
      )}
    </div>
  )
}
