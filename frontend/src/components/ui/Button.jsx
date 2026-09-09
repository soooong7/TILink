import styles from './Button.module.css';

export default function Button({
  variant = 'primary',
  block = false,
  className = '',
  type = 'button',
  ...props
}) {
  const classes = [styles.button, styles[variant], block ? styles.block : '', className]
    .filter(Boolean)
    .join(' ');

  return <button type={type} className={classes} {...props} />;
}
