import styles from './Field.module.css';

/** label 과 컨트롤을 한 쌍으로 묶는다. children 을 주면 그것을, 없으면 input 을 렌더한다. */
export default function Field({ label, htmlFor, children, className = '', ...inputProps }) {
  return (
    <div className={`${styles.field} ${className}`.trim()}>
      <label className={styles.label} htmlFor={htmlFor ?? inputProps.name}>
        {label}
      </label>
      {children ?? <input id={inputProps.name} className={styles.control} {...inputProps} />}
    </div>
  );
}
